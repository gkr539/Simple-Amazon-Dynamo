package edu.buffalo.cse.cse486586.simpledynamo;

import java.util.Comparator;

public class Message implements Comparable<Message> {
    String key;
    String value;
    String origin;
    String type;
    String version;

    public String getKey(){
        return key;
    }

    public String getVersion(){
        return version;
    }

    public void setVersion( String version ){
        this.version = version;
    }

    @Override
    public String toString(){
        return "Message{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", origin='" + origin + '\'' +
                ", type='" + type + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public String getOrigin(){
        return origin;
    }

    public void setOrigin( String origin ){
        this.origin = origin;
    }

    public String getType(){
        return type;
    }

    public void setType( String type ){
        this.type = type;
    }

    public void setKey( String key ){
        this.key = key;
    }

    public String getValue(){
        return value;
    }

    public void setValue( String value ){
        this.value = value;
    }



    @Override
    public int compareTo( Message another ){

        int temp =  this.key.compareTo( another.key );
        if (temp == 0) {
            temp = this.version.compareTo( another.version );
        }

        return temp;
    }
}

