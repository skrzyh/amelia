/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.neology.net;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author obsidiam
 */
public class Closed extends TransportState{
    @Override
    public void closeConnection(Transport t, Socket s) {
        try {
            System.out.println("Closed::closeConnection() -> attempting to close...");
            if(t.isConnected()){
                t.close();
                s.close();
                System.out.println("Closed::closeConnection() -> closed.");
            }
        } catch (IOException ex) {
            Logger.getLogger(Closed.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    @Override
    public void haltConnection(Transport t, Socket s) {
        try {
            t.close();
            s.close();
        } catch (IOException ex) {
            Logger.getLogger(Closed.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
