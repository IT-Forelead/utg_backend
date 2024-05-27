import Dependencies.*

name := "mailer"
libraryDependencies ++= Seq(
  javax.mail,
  Dependencies.io.estatico.newtype,
)

dependsOn(LocalProject("common"))
