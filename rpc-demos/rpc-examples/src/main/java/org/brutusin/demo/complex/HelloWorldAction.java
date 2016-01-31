package org.brutusin.demo.complex;

import org.brutusin.rpc.Server;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;

public class HelloWorldAction extends SafeAction<Person[], Greeting[]> {

    @Override
    public Cacheable<Greeting[]> execute(Person[] list) {
        if (list == null) {
            list = new Person[1];
        }
        Greeting[] ret = new Greeting[list.length];
        for (int i = 0; i < list.length; i++) {
            ret[i] = greetPerson(list[i]);
        }
        return Cacheable.forForever(ret);
    }

    private Greeting greetPerson(Person person) {
        StringBuilder sb = new StringBuilder("Hello");
        Greeting ret = new Greeting();
        if (person != null) {
            if (person.getName() != null) {
                sb.append(" ");
                sb.append(person.getName());
            }
            if (person.getAge() != null) {
                sb.append(", you are ");
                sb.append(person.getAge());
                sb.append(" years old");
            }
        }
        sb.append("!");
        ret.setGreeting(sb.toString());
        return ret;
    }

    public static void main(String[] args) {
        Server.test(new HelloWorldAction());
    }
}
