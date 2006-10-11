/**
 * Copyright (c) 2000-2006, Identyx Corporation.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.safehaus.penrose.studio.project;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.safehaus.penrose.management.PenroseClient;

public class Project {

	private String name;
    private String type = PenroseClient.PENROSE;
	private String host = "localhost";
    private int port = 0;
    private String username;
    private String password;

    public Project() {
    }

    public Project(Project project) {
        name = project.getName();
        type = project.getType();
        host = project.getHost();
        port = project.getPort();
        username = project.getUsername();
        password = project.getPassword();
    }
    
	public Element toElement() {
		Element element = new DefaultElement("project");
		element.addAttribute("name", name);
        element.addAttribute("type", type);
		element.addAttribute("host", host);
        if (port > 0) element.addAttribute("port", ""+port);
		element.addAttribute("username", username);
		element.addAttribute("password", password);
		return element;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
