import com.typesafe.sbt.packager.Keys.*
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerChmodType
import sbt.*
import sbt.Keys.*

object DockerImagePlugin extends AutoPlugin {
  val DOCKER_REPOSITORY: Option[String] = sys.env.get("AWS_ECR_REPOSITORY")
  val baseImageName: String = sys
    .env
    .getOrElse(
      "DOCKER_IMAGE_NAME",
      s"${DOCKER_REPOSITORY.getOrElse("")}/openjdk:17",
    )
  object autoImport {
    lazy val generateServiceImage: TaskKey[Unit] =
      taskKey[Unit]("Generates an image with the native binary")
  }
  override def projectSettings: Seq[Def.Setting[?]] =
    Seq(
      dockerBaseImage  := baseImageName,
      dockerRepository := DOCKER_REPOSITORY,
      dockerChmodType  := DockerChmodType.UserGroupWriteExecute,
    )

  def serviceSetting(serviceName: String): Seq[Def.Setting[?]] =
    Seq(
      Docker / packageName         := s"utg/$serviceName",
      packageDoc / publishArtifact := false,
      packageSrc / publishArtifact := true,
      publish / skip               := false,
    )

  override def requires: sbt.Plugins =
    JavaAppPackaging && DockerPlugin
}
