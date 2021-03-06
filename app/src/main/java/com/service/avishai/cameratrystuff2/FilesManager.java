package com.service.avishai.cameratrystuff2;

import android.content.Context;

import java.io.File;


/**
 * Created by avishai on 3/10/2017.
 */

public class FilesManager {


    private FilesManager(){

    }

    //check if file exists, if so, add integer to name
    public static String getFilePath(String filePath){

        File f = new File(filePath);
        String folder = f.getParent();
        String fileName = f.getName();

        Integer counter = 1;
        while(f.exists()){
            String nameWithNoExtension = fileName.substring(0, fileName.lastIndexOf("."));
            nameWithNoExtension += counter.toString();

            f = new File(folder + "/" + nameWithNoExtension + ".mp4");
            counter++;
        }

        return f.getPath();
    }

    public static String merge2Videos(String video1, String video2){
        return merge2Videos(video1, video2, false, null);
    }

    public static String merge2Videos(String video1, String video2, Boolean useFFmpeg, Context context){

        String VideoPath = String.format("/sdcard/slangifyMerged%s.mp4", String.valueOf(System.currentTimeMillis()));



        boolean answer = false;
        if(useFFmpeg){

            answer = MediaEditUtil.merge2VideosFFMPEG(video1, video2, VideoPath, context);
        }
        else{
            //use MP4Parser
            answer = MediaEditUtil.mergeVideos(video1, video2, VideoPath);
        }

        return !answer ? "" : VideoPath;
    }





}
