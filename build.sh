#!/bin/bash


if [[ "$*" == *"--no-user"* ]]
then
  USER_OPTION=""
else
if [ -z ${USER_UID:+x} ]
then
  export USER_UID=1000
  export GROUP_GID=1000
fi
  USER_OPTION="-u $USER_UID:$GROUP_GID"
fi


clean () {
  docker compose run --rm $USER_OPTION gradle gradle clean
}

install() {
  docker compose run --rm $USER_OPTION gradle gradle install publishToMavenLocal
}

testGradle () {
  ./gradlew "$GRADLE_OPTION"test
}

publish() {
  if [ -e "?/.gradle" ] && [ ! -e "?/.gradle/gradle.properties" ]
  then
    echo "odeUsername=$NEXUS_ODE_USERNAME" > "?/.gradle/gradle.properties"
    echo "odePassword=$NEXUS_ODE_PASSWORD" >> "?/.gradle/gradle.properties"
    echo "sonatypeUsername=$NEXUS_SONATYPE_USERNAME" >> "?/.gradle/gradle.properties"
    echo "sonatypePassword=$NEXUS_SONATYPE_PASSWORD" >> "?/.gradle/gradle.properties"
  fi
  docker compose run --rm $USER_OPTION gradle gradle publish
}

for param in "$@"
do
  case $param in
    '--no-user')
      ;;
    clean)
      clean
      ;;
    install)
      install
      ;;
    test)
      testGradle
      ;;
    publish)
      publish
      ;;
    *)
      echo "Invalid argument : $param"
  esac
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done

