package com.derekmorrison.movieref;

public class Globals{
    private static Globals instance;

    // Global variables
    private boolean prefChanged = true;
    private boolean hasDataConnection = false;

    // Restrict the constructor from being instantiated
    private Globals(){}

    public void setPrefChanged(boolean b){
        this.prefChanged=b;
    }
    public boolean getPrefChanged(){
        return this.prefChanged;
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
