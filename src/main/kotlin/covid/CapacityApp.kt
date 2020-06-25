package covid

import com.fasterxml.jackson.module.kotlin.*
import org.http4k.client.ApacheClient
import org.http4k.core.Status
import space.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

fun getCovidCapacity(location: Location): Int {
    val re = """covidCapacity\: (\d+)""".toRegex()
    val result = re.find(location.description)?.groupValues
    if (null == result || result.size != 2) {
        return 0
    }
    return result[1].toInt()
}


fun main(args: Array<String>) {
    val spaceName: String = System.getenv("COVID_SPACENAME") ?: ""
    val token = System.getenv("COVID_SPACETOKEN") ?: ""

    if (spaceName == "" || token == "") {
        println("COVID_SPACENAME and COVID_SPACETOKEN environmental variables must be set")
        exitProcess(1)
    }

    val mapper = jacksonObjectMapper()
    val client = ApacheClient()
    val space = SpaceRequest(spaceName, token)

    val response = client(space.Stats())
    if (response.status != Status.OK) {
        println("Could not fetch space status. Response code: ${response.status}")
        exitProcess(1)
    }

    val locationRequest = space.TeamDirectory().getAllLocations(listOf("id", "name", "description"),false)

    val locations = mapper.readValue<List<Location>>(client(locationRequest).bodyString()).filter { location ->
        location.description.contains("covidCapacity")
    }

    val tomorrow = LocalDate.now().plusDays(1)
    val outset = LocalDate.now().plusDays(7)

    val memberLocations = space.TeamDirectory().getAllMemberLocations(listOf("data(location(id,description,name),member(id,name),since,till),next,totalCount"), tomorrow, outset)
    val query = mapper.readValue<MemberLocationQuery>(client(memberLocations).bodyString())

    val dateLocations = mutableMapOf<LocalDate, MutableMap<Location, MutableSet<Member>>>()

    for (memberLocation in query.data) {
        var till = LocalDate.parse(memberLocation.till.iso, DateTimeFormatter.ISO_DATE)
        var since = LocalDate.parse(memberLocation.since.iso, DateTimeFormatter.ISO_DATE)
        if (since <= tomorrow) {
            since = tomorrow
        }

        if (till > outset) {
            till = outset
        }

        for (date in since..till) {
            val dateValue = dateLocations.getOrPut(date, { mutableMapOf<Location, MutableSet<Member>>() })
            if (memberLocation.location in locations) {
                val locationData = dateValue.getOrPut(memberLocation.location, { mutableSetOf() })
                locationData.add(memberLocation.member)
            }
        }
    }

    for (data in dateLocations) {
        for (d in data.value) {
            if (getCovidCapacity(d.key) < d.value.size) {
                val msg = "There are too many people at ${d.key.name} on ${data.key}. Please update your status if you do not have to attend in person"
                println(msg)
                for (member in d.value) {
                    client(space.Chats().Messages().sendMessage(ChatsMemberMessage(member.id, msg)))
                }
            }
        }
    }
}