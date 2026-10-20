package org.example.config;

import com.beust.jcommander.Parameter;

public class Args {

    @Parameter(names = "-help",help = true)
    public boolean help;

    @Parameter(names = {"-p", "-port"}, description = "Port Number")
    public Integer port=6969;

}
