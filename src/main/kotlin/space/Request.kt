package space

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.string
import java.time.LocalDate
import java.time.format.DateTimeFormatter


open class SpaceRequest constructor(spaceName: String, token: String) {
    val spaceName = spaceName
    val token = token
    val acceptHeader = Header.required("Accept")
    val bearerHeader = Header.required("Authorization")
    val contentTypeHeader = Header.optional("Content-Type")
    val fieldLens = Query.string().optional("\$fields")
    val queryLens = Query.string().required("query")

    fun baseRequest(url: String, method: Method = Method.GET): Request {
        val spaceUrl = "https://${spaceName}.jetbrains.space" + url
        return Request(method, spaceUrl).with(
                bearerHeader of "Bearer " + token,
                acceptHeader of "application/json"
        )
    }

    inner class TeamDirectory {
        var sinceLens = Query.string().optional("since")
        var tillLens = Query.string().optional("till")
        val withArchivedLens = Query.boolean().required("withArchived")

        fun getAllLocations(fields: List<String>, withArchived: Boolean): Request {
            val url = "/api/http/team-directory/locations"

            return baseRequest(url).with(
                    queryLens of "",
                    withArchivedLens of withArchived,
                    fieldLens of fields.joinToString(",")
            )
        }

        fun getAllMemberLocations(fields: List<String>, since: LocalDate, till: LocalDate): Request {
            val url = "/api/http/team-directory/member-locations"

            return baseRequest(url).with(
                    sinceLens of since.format(DateTimeFormatter.ISO_DATE),
                    tillLens of till.format(DateTimeFormatter.ISO_DATE),
                    fieldLens of fields.joinToString(",")
            )
        }
    }

    inner class Chats {
        inner class Messages {
            fun sendMessage(message: ChatsMessage) : Request {
                val url = "/api/http/chats/messages/send-message"
                val req = baseRequest(url, Method.POST)
                return req.body(message.json()).with(
                        contentTypeHeader of "application/json"
                )
            }
        }

    }

    fun Stats() : Request {
        return baseRequest("/api/http/team-directory/stats")
    }
}

class ChatsMemberMessage(recipientID: String, message: String) : ChatsMessage {
    val type = ChatsMessageRecipient.MEMBER
    val recipientID = recipientID
    val message = message

    override fun recipientType(): ChatsMessageRecipient {
        return type
    }

    override fun value(): String {
        return ""
    }

    override fun json(): String {
        return """{
      "recipient": {
        "className": "MessageRecipient.Member",
        "member": "${recipientID}"
      },
      "content": {
        "className": "ChatMessage.Text",
        "text": "${message}"
      }
    }"""
    }
}

interface ChatsMessage {
    fun recipientType() : ChatsMessageRecipient
    fun value() : String
    fun json(): String
}



enum class ChatsMessageRecipient {
    CHANNEL {
        override fun toString(): String {
            return "MessageRecipient.Channel"
        }
    },
    CODE_REVIEW {
        override fun toString(): String {
            return "MessageRecipient.CodeReview"
        }
    },
    ISSUE {
        override fun toString(): String {
            return "MessageRecipient.Issue"
        }
    },
    MEMBER {
        override fun toString(): String {
            return "MessageRecipient.Member"
        }
    }
}

data class NameDetail(
        val firstName: String,
        val lastName: String
)

data class MemberLocationQuery(
        val next: Int,
        val totalCount: Int,
        var data: List<MemberLocation>
)

data class MemberLocation(
        val location: Location,
        val member: Member,
        val since: SpaceDate,
        val till: SpaceDate
)

data class SpaceDate(
        val iso: String,
        val year: Int,
        val month: Int,
        val day: Int
)

class DateIterator(val startDate: LocalDate,
                   val endDateInclusive: LocalDate,
                   val stepDays: Long) : Iterator<LocalDate> {
    private var currentDate = startDate

    override fun hasNext() = currentDate <= endDateInclusive

    override fun next(): LocalDate {

        val next = currentDate

        currentDate = currentDate.plusDays(stepDays)

        return next
    }
}

class DateProgression(override val start: LocalDate,
                      override val endInclusive: LocalDate,
                      val stepDays: Long = 1) :
        Iterable<LocalDate>, ClosedRange<LocalDate> {

    override fun iterator(): Iterator<LocalDate> =
            DateIterator(start, endInclusive, stepDays)

    infix fun step(days: Long) = DateProgression(start, endInclusive, days)

}

operator fun LocalDate.rangeTo(other: LocalDate) = DateProgression(this, other)
data class Location(
        val id: String,
        val name: String,
        val description: String
)

data class Member(
        val id: String,
        val name: NameDetail
)