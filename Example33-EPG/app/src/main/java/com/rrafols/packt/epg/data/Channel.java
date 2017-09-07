package com.rrafols.packt.epg.data;

import java.util.ArrayList;

public class Channel {
    private String name;
    private String iconUrl;
    private ArrayList<Program> programs;

    public Channel(String name, String iconUrl) {
        this.name = name;
        this.iconUrl = iconUrl;
        this.programs = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public void addProgram(Program program) {
        programs.add(program);
    }

    public ArrayList<Program> getPrograms() {
        return programs;
    }
}
