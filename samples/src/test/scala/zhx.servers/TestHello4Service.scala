package zhx.servers

import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import zhx.auth.{AuthenticationHeaders, Authenticator}
import zhx.encoding.Encoders._
import zhx.servers.Middlewares.withMiddleware
import zio._
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, Predicate, assert, fail, suite, testM}
import MoreMiddlewares._

object TestHello4Service extends DefaultRunnableSpec(
  suite("routes suite")(
    testM("president returns donald") {
      val req1 = Request[withMiddleware.AppTask](Method.GET, Uri.uri("/president"))
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
      (for{
        response <- hello4Service.run(req)
        body <- response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
        parsed <- parseIO(body)
      }yield parsed)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
        .fold(
          e => fail(Cause.fail(e)),
          s => assert(s, Predicate.equals(Person.donald)))
    },
    testM("joe is 76") {
      val req1 = Request[withMiddleware.AppTask](Method.POST, Uri.uri("/ageOf"))
      val req = AuthenticationHeaders.addAuthentication(req1, "tim", "friend")
        .withEntity(Person.joe)
      (for{
        response <- hello4Service.run(req)
        body <- response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
      }yield body)
        .provide(new Authenticator{ override val authenticatorService = Authenticator.friendlyAuthenticator})
        .fold(
          e => fail(Cause.fail(e)),
          s => assert(s, Predicate.equals("76")))
    }

  ))


object MoreMiddlewares {
  val hello4Service1 = new Hello4Service[Authenticator]
  // todo - is there a better way to add isNotFound to the middleware service?
  val hello4Service = Router[withMiddleware.AppTask](
    ("" -> withMiddleware.authenticationMiddleware(hello4Service1.service)))
    .orNotFound

}
