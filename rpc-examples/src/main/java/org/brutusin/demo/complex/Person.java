package org.brutusin.demo.complex;

import org.brutusin.json.annotations.JsonProperty;

public class Person {

    @JsonProperty(required = true, title = "Name", description = "This is a description of the name field supporting [`markdown` syntax](https://daringfireball.net/projects/markdown/)")
    private String name;
    @JsonProperty(required = true, title = "Age")
    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
