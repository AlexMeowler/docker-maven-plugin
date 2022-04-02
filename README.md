
## Docker Maven Plugin

A simple Maven plugin to automatically rebuild image and/or run it in container after building project executable file.

## Prerequisites

- Maven 3.8.3 or higher 
- Java 11 or higher

##Usage

Just copy this into your project POM. `containerName` and `imageName` are mandatory parameters!

```
   <plugin>
        <groupId>org.retal</groupId>
        <artifactId>docker-maven-plugin</artifactId>
        <version>0.3.0</version>
        <executions>
            <execution>
            <goals>
                <goal>docker-update-container</goal>
            </goals>
            <configuration>
                <containerName>containerName</containerName>
                <imageName>imageName</imageName>
            </configuration>
            </execution>
        </executions>
    </plugin>
```