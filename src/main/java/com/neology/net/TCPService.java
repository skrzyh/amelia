/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.neology.net;

import abstracts.LocalEnvironment;
import com.neology.data.ConnectionDataHandler;
import com.neology.data.ImageDataHandler;
import com.neology.exceptions.TransportException;
import enums.Local;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;

/**
 *
 * @author obsidiam
 */
   public class TCPService extends Service{
      DataInputStream din;
      DataOutputStream dout;
      private Connection CON;
      private String NAME;
      private int INDEX = 0;
      private HashMap<String,Image> IMAGES = new HashMap<>();
      private BaudrateMeter MTR;
      private ImageDataHandler IDH = ImageDataHandler.getInstance();
      private ConnectionDataHandler CDH = ConnectionDataHandler.getInstance();
      private LocalEnvironment LOCAL = new LocalEnvironment() {};
      
      protected TCPService(Connection c,int index){
          this.CON = c;
          this.INDEX = index;
          this.MTR = CON.getTranportInstance().getBaudrateMeter();
      }
      
      private Image processData(byte[] buffer) throws IOException {
                ByteArrayInputStream is;
                byte[] result;
                int len = (int)buffer[0]+1;
                char[] c = new char[len];

                for(int i = 1; i<len; i++){
                    c[i] = (char)((int)buffer[i]);
                }

                String name = String.valueOf(c).trim();
                NAME = name;
                System.out.println("FILE_NAME_RECEIVED: "+name);
                result = Arrays.copyOfRange(buffer,len,8192);
                is = new ByteArrayInputStream(result);
                
                BufferedImage bf = ImageIO.read(is);
                System.out.println("AVAILABLE_BYTE_COUNT: "+is.available());
                ImageIO.write(bf, "JPG", new FileOutputStream(LOCAL.getLocalVar(Local.TMP)+File.separator+name+".jpg"));
                Image out = new Image("file:///"+LOCAL.getLocalVar(Local.TMP)+File.separator+name+".jpg",150,100,true,true);
                System.err.println("ERROR: "+out.isError());
                System.out.println("LDR_STATE: "+out.getProgress());
                System.out.println("Buffer: "+buffer.length);
                MTR.count(buffer.length);
                MTR.stopMeasuringCycle();
                return out;
        }

        @Override
        protected Task createTask() {
            return new Task<Void>(){
                @Override
                protected Void call() throws Exception {
                    while(!this.isCancelled()){
                       MTR.startMeasuringCycle();
                       Established e = new Established();
                       CON.changeState(e);

                       Transport t = CON.getTranportInstance();
                       System.out.println("TRANSPORTER_IP: "+t.getIp());

                       byte[] buffer;
                       try {
                           buffer = CON.read(t);
                           Image im = processData(buffer);
                           IDH.getImagesMap().put(NAME, im);
                           
                           String data = "";
                           data += "IP: "+t.getIp().substring(1)+",";
                           data += "Speed: "+t.getBaudrateMeter().kBPS()+"kB/s,";
                           data += "Is connected: "+t.isConnected()+",";
                           data += "Was connected earlier: "+t.wasConnected();
                           //System.out.println(data);
                           CDH.getData().add(data);
                           //IDH.setImagesMap(IMAGES);
                          
                       } catch (TransportException | IOException ex) {
                           System.err.println("LOCALIZED_ERR_MSG:"+ex.getLocalizedMessage());
                           CDH.getThreadsMap().forEach((desc,serv) ->{
                               if(desc.equals(t.getIp())){
                                   serv.cancel();
                               }
                           });
                           CDH.getThreadsMap().remove(t.getIp());
                       }
                    }
                    Closed c = new Closed();
                    CON.changeState(c);
                    CON.close();
                    return null;
                }
            };
        }
        
        public String getClientMachineName(){
            return NAME;
        }
   }
