# labai-jaxb-jsr305

## About
Extension for jaxb maven plugin for generating @NotNull annotation on mandatory fields.

### Purpose

When kotlin access Java classes, it assumes that every field is not null by default. 
In most cases it is not true for classes, generated from xsd. 
If to reverse to nullable by default, it also will not always be true, as some fields 
are not nullable by xsd scheme.

This plugin adds @NotNull annotation on those fields, which are mandatory by xsd. 
It adds nullable-by-default annotation on package level also.


### Usage

Add configuration to maven jaxb plugin in pom.xml:

com.github.labai:labai-jsr305-jaxb-plugin
com.github.labai:labai-jsr305x-annotations

And add argument _-XJsr305Annotations_.

See samples/jaxb-kotlin-sample.

```xml
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <version>0.13.0</version>
    <configuration>
        <args>
            <arg>-extension</arg>
            <arg>-XJsr305Annotations</arg>
            <arg>-XJsr305Annotations:generateListItemNonnull=true</arg>
        </args>
    </configuration>

    <executions>
        <execution>
            <id>xsd1</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <schemaDirectory>src/main/resources/xsd/samplexsd</schemaDirectory>
                <generatePackage>sample.generated.samplexsd</generatePackage>
                <forceRegenerate>true</forceRegenerate>
            </configuration>
        </execution>
    </executions>

    <dependencies>
        <dependency>
            <groupId>com.github.labai</groupId>
            <artifactId>labai-jsr305-jaxb-plugin</artifactId>
            <version>0.0.3</version>
        </dependency>
        <dependency>
            <groupId>com.github.labai</groupId>
            <artifactId>labai-jsr305x-annotations</artifactId>
            <version>0.0.2</version>
        </dependency>
    </dependencies>
</plugin>
```

Also you will need to add dependency:
```xml
<dependency>
    <groupId>com.github.labai</groupId>
    <artifactId>labai-jsr305x-annotations</artifactId>
    <version>0.0.2</version>
</dependency>
```

### Parameters

You may want to use another NotNull annotation 
(e.g. kotlin supports a couple of them: https://kotlinlang.org/docs/java-interop.html#nullability-annotations).
Then you may provide additional parameters:

| Parameter | Description | Default
| :--- | :--- | :---
| -XJsr305Annotations:nonnullClass | @Notnull annotation on field, method or parameter | @com.github.labai.jsr305x.api.NotNull
| -XJsr305Annotations:defaultNullableClass | @NullableByDefault on package-info.java (default for all package) | @com.github.labai.jsr305x.api.NullableByDefault
| -XJsr305Annotations:generateDefaultNullable | generate @NullableByDefault | true |
| -XJsr305Annotations:generateListItemNonnull | generate @NotNull for list items, e.g. List<@NotNull Item> | false |
| -XJsr305Annotations:verbose | More logs | true |

An example:

```xml
<arg>-XJsr305Annotations:defaultNullableClass=org.your.DefaultNullable</arg>
<arg>-XJsr305Annotations:nonnullClass=org.your.NonNull</arg>
<arg>-XJsr305Annotations:generateListItemNonnull=true</arg>
```

