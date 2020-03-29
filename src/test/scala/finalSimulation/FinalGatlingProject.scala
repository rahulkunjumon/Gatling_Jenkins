package Simulations

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

class FinalGatlingProject extends Simulation{

  def getProperty(propertyName: String,defaultValue: String)={
    Option(System.getenv(propertyName))
      .orElse( Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  def userCount():Int = getProperty("USERCOUNT",defaultValue = "5").toInt
  def rampDuration():Int = getProperty("RAMPDURATION",defaultValue = "10").toInt
  def totalDuration():Int = getProperty("TOTALDURATION",defaultValue = "60").toInt

//  var idNumbers = (15 to 500).iterator
  var rand = new Random();
  val now = LocalDate.now()
  var pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def randomNumber(length: Int) = {
    rand.nextInt(length)
  }

  def randomName(length: Int) = {
    rand.alphanumeric.filter(_.isLetter).take(length).mkString
  }
  def getRandomDate(startDate: LocalDate, random: Random): String = {
    startDate.minusDays(random.nextInt(30)).format(pattern)
  }
val customfeeder = Iterator.continually(Map(
  "gameId" ->  randomNumber(999),
  "name" -> ("Game-" +randomName(6)),
  "releaseDate" -> getRandomDate(now, rand),
  "reviewScore" -> rand.nextInt(100),
  "category" -> ("Category-" + randomName(6)),
  "rating" -> ("Rating-" + randomName(4))
))

  val httpconf = http.baseUrl("http://localhost:8080/app/")
    .header("accept","application/Json")

  def getAllGames() = {
    exec(http("01_001_GetAllGames")
      .get("videogames")
      .check(status.is("200"))
//      .check(jsonPath("$[0].name").is("Resident Evil 4"))
      .check(jsonPath("$[10].id").saveAs("idValue"))
      .check(jsonPath("$[9].id").saveAs("idDeleteValue")))
  }
def addNewGame() ={
  println("gameId : "+randomNumber(999))
  println("name : "+"Game-" +randomName(6))
  println("releaseDate : "+getRandomDate(now, rand))
  println("reviewScore : "+rand.nextInt(100))
  println("category : "+"Category-" + randomName(6))
  println("rating : "+"Rating-" + randomName(4))

  feed(customfeeder)
  .exec(http("01_002_AddNewGames")
    .post("videogames/")
    .body(ElFileBody("bodies/NewGameTemplate.json")).asJson
    .check(status.is("200"))
    .check(jsonPath("$.status").is("Record Added Successfully")))

}
  def getSpecificGame() ={
    exec(http("01_003_GetSpecificGame")
      .get("videogames/${idValue}")
      .check(status.is("200")))
  }
  def ModifySpecificGame() ={
    feed(customfeeder)
    .exec(http("01_004_ModifySpecificGame")
      .put("videogames/${idValue}")
      .body(StringBody(
                                "{" +
                                "\n\t\"id\": ${idValue}," +
                                "\n\t\"name\": \"${name}\"," +
                                "\n\t\"releaseDate\": \"${releaseDate}\"," +
                                "\n\t\"reviewScore\": ${reviewScore}," +
                                "\n\t\"category\": \"${category}\"," +
                                "\n\t\"rating\": \"${rating}\"\n}")
                  ).asJson
      .check(status.is("200")))
  }

  def deleteSpecificGame() = {
    exec(http("01_005_DeleteGames")
        .delete("videogames/${idDeleteValue}")
        .check(jsonPath("$.status").is("Record Deleted Successfully")))
  }

    println("id Value generated is :  ${idValue}")
    val scn = scenario(scenarioName = "Final Gatling Test")
    .forever(){
      exec(addNewGame())
        .pause(10.seconds)
      .exec(getAllGames())
        .pause(5.seconds)
        .exec(getSpecificGame())
        .pause(5.seconds)
        .exec(deleteSpecificGame())
        .pause(5.seconds)
        .exec(ModifySpecificGame)
        .pause(5.seconds)
    }
  setUp(
    scn.inject(
      nothingFor(5.seconds),
      rampUsers(userCount) during(rampDuration)
    ).protocols(httpconf.inferHtmlResources())
  ).maxDuration(totalDuration())
}

