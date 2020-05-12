# jenkinsfile generator
Reducing Boilerplate Code with jenkinnsfile maven plugin
> More Time for Feature and functionality
  Through a simple set of jenkinsfile templates and saving 60% of development time 

## How to use

```
<properties>
    <!-- Jenkinsfile properties -->
    <jenkinsfile.app-name>${project.artifactId}</jenkinsfile.app-name>
    ...
</properties>
<plugins>
    <plugin>
        <groupId>de.microtema</groupId>
        <artifactId>jenkinsfile-maven-plugin</artifactId>
        <version>2.0.0</version>
        <configuration>
            <appName>${jenkinsfile.app-name}</appName>
            ...
        </configuration>
        <executions>
            <execution>
                <id>jenkinsfile</id>
                <phase>compile</phase>
                <goals>
                    <goal>generate</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</plugins>
```
    
## How to use with annotations?

*  @Model Person person; // create person instance within filled required properties
*  @Models Collection< Person > persons; // create a List of person instances with random **[1..10]** size
  
## How to use with annotations but with custom builder?

*  @Resource PersonModelBuilder builder; //custom model builder
*  @Model Person person; // create person instance within filled required properties
*  @Models Collection< Person > persons; // create a List of person instances with random **[1..10]** size

## How Annotated fields are injected?

>  @Before public void setUp() {
>      FieldInjectionUtil.injectFields(this);
>  }

## Supported out of the box types

* @Model Byte byte;
* @Model Boolean boolean;
* @Model Character character;
* @Model Date date;
* @Model BigDecimal bigDecimal;
* @Model Double double;
* @Model Float float;
* @Model Integer integer;
* @Model Long long;
* @Model String string;
* @Model URL url;
* @Model Map<K, V> map;
* @Model Enum enum;
    
## Technology Stack

* Java 1.8
    * Streams 
    * Lambdas
* Third Party Libraries
    * Commons-BeanUtils (Apache License)
    * Commons-IO (Apache License)
    * Commons-Lang3 (Apache License)
    * Junit (EPL 1.0 License)
* Code-Analyses
    * Sonar
    * Jacoco
    
## License

MIT (unless noted otherwise)

