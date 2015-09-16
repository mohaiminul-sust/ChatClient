/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mcliclient;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author OSMAN
 */
public class Play_sound {
    public  void play(){
        File file=new File("RecordAudio.wav");
        PlaySound(file);
    }
     static void PlaySound(File sound)
     {
         try{
             Clip clip=AudioSystem.getClip();
             clip.open(AudioSystem.getAudioInputStream(sound));
             clip.start();
             Thread.sleep(clip.getMicrosecondLength()/1000);
         }catch(LineUnavailableException | UnsupportedAudioFileException | IOException | InterruptedException e){
             System.out.println(e);
     }
}
}