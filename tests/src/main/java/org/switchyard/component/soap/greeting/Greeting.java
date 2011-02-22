/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.switchyard.component.soap.greeting;

import java.util.Date;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Greeting {

    private Person _person;
    private Date _time;

    /**
     * Get person.
     * @return person.
     */
    public Person getPerson() {
        return _person;
    }

    /**
     * Set person.
     * @param person Person.
     */
    public void setPerson(Person person) {
        this._person = person;
    }

    /**
     * Get time.
     * @return time.
     */
    public Date getTime() {
        return _time;
    }

    /**
     * Set time.
     * @param time Time.
     */
    public void setTime(Date time) {
        this._time = time;
    }
}