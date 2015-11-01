package com.derekmorrison.movieref;

public class Globals{
    private static Globals instance;

    // Global variables
    private boolean RefreshNeeded = true;
    private boolean hasDataConnection = false;

    // Restrict the constructor from being instantiated
    private Globals(){}

    public void setRefreshNeeded(boolean b){
        this.RefreshNeeded =b;
    }
    public boolean getRefreshNeeded(){
        return this.RefreshNeeded;
    }

    public void setDataConnection(boolean b){
        this.hasDataConnection=b;
    }
    public boolean getDataConnection(){
        return this.hasDataConnection;
    }

    public static synchronized Globals getInstance(){
        if(instance==null){
            instance=new Globals();
        }
        return instance;
    }
}
