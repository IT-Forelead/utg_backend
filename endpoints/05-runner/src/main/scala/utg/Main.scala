package utg

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.effect.std.Random
import cats.effect.std.Dispatcher
import cats.implicits.toTraverseOps
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import utg.setup.Environment

object Main extends IOApp {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private def runnable: Resource[IO, List[IO[ExitCode]]] =
    for {
      implicit0(dispatcher: Dispatcher[IO]) <- Dispatcher.parallel[IO]

      env <- Environment.make[IO]
      implicit0(random: Random[IO]) <- Resource.eval(Random.scalaUtilRandom[IO])
      httpModule <- HttpModule.make[IO](env.toServer)
    } yield List(httpModule)

  override def run(
      args: List[String]
    ): IO[ExitCode] =
    runnable.use { runners =>
      for {
        fibers <- runners.traverse(_.start)
        _ <- fibers.traverse(_.join)
        _ <- IO.never[ExitCode]
      } yield ExitCode.Success
    }
}
