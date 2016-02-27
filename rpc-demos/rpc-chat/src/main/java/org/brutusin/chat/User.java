/*
 * Copyright 2016 Ignacio del Valle Alles idelvall@brutusin.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.chat;

import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class User {

    private static final AtomicInteger counter = new AtomicInteger();
    private final Integer id;

    private User() {
        this.id = counter.incrementAndGet();
    }

    public Integer getId() {
        return id;
    }

    public static User from(HttpSession httpSession) {
        synchronized (httpSession) {
            User user = (User) httpSession.getAttribute(User.class.getName());
            if (user == null) {
                user = new User();
                httpSession.setAttribute(User.class.getName(), user);
            }
            return user;
        }
    }
    public static void main(String[] args) {
          for (int i = 4; i < 80; i++) {
            System.out.print(i+",");
        }
    }
}
