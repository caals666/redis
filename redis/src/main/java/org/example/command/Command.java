package org.example.command;

import org.example.protocol.RespType;
import org.example.protocol.RespValue;

import java.util.*;

public class Command {
    private final String name;
    private final List<String> args;

    public Command(String name, List<String> args) {
        this.name = name;
        this.args = args;
    }

    public static Command fromRespValue(RespValue value) {
        if(value==null){
            return null;
        }

        if(value.getType()== RespType.ARRAY){
            List<RespValue> elements = value.getArrayValue();
            if(elements==null){
                return null;
            }
            String cmdName = elements.getFirst().getStringValue();
            System.out.println("cmdName: "+cmdName);
            List<String> cmdArgs = new ArrayList<>();
            for (int i=1;i<elements.size();i++) {
                RespValue elem = elements.get(i);
                cmdArgs.add(elem.getStringValue() != null ? elem.getStringValue() : elem.getIntegerValue() != null ? elem.getIntegerValue().toString() : "");
            }
            return new Command(cmdName, cmdArgs);
        }

        if(value.getType()==RespType.BULK_STRING||value.getType()==RespType.SIMPLE_STRING){
            String raw = value.getStringValue();
            if(raw==null){
                return null;
            }
            String[] parts = raw.trim().split("\\s+");
            List<String> cmdArgs = new ArrayList<>(Arrays.asList(parts));
            return new Command(parts[0], cmdArgs);
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public List<String> getArgs() {
        return args;
    }

    public int getArgCount() {
        return args.size();
    }

    public String getArg(int index) {
        if (index >= 0 && index < args.size()) {
            return args.get(index);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Command{" +
                "name='" + name + '\'' +
                ", args=" + args +
                '}';
    }
}
