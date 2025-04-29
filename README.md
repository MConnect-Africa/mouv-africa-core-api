# mouv


## How To Set Up

### PREEQUISITES
  - Have Java Installed >= 11
  - Have Maven Set up
  - Have JAV_HOME envar set (for the first three documentation is available online)
  - Create a file `~/.m2.settings.xml` and add the following :-

```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">

   <activeProfiles>
      <activeProfile>github</activeProfile>
   </activeProfiles>

   <profiles>
        <profile>
            <id>github</id>
            <repositories>
                <repository>
                    <id>central</id>
                    <url>https://repo1.maven.org/maven2</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
                <repository>
                    <id>github-laxnit</id>
                    <name>GitHub Packages Laxnit</name>
                    <url>https://maven.pkg.github.com/T-Tech-LTD/laxnit-backend-utils</url>
                </repository>
            </repositories>
        </profile>
    </profiles>

    <servers>
        <server>
            <id>github-laxnit</id>
            <username>DK-denno</username>
            <password>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</password>
        </server>
   </servers>
</settings>
```
- cd into the mouv code base.
- Export envars as outline in the projects .env file.
- Run `mvn clean install -DskipTests` to install the packages imported in the codebase
- Run `mvn clean compile vertx:run` To sun the code base locally
