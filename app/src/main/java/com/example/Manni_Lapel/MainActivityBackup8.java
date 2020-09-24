package com.example.Manni_Lapel;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class MainActivityBackup8 extends Activity {

    private static final String TAG = MainActivityBackup8.class.getSimpleName();
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";

    FileOutputStream os = null;

    int bufferSize ;
    int frequency = 44100; //8000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    boolean started = false;
    RecordAudio recordTask;

    int WIFI_SECS=30;

    //How resopnsive it is to silence.
    int SILENCE_LOOPS=30;
    int threshold=9000;
    private WifiManager wifi;


    boolean debug=false;



    boolean stopped = false;

    // globally


    public TextView tvErrors;
    public TextView tvLastUpload;
    public TextView tvBreaker;

    public EditText edThreshold;
    public RadioButton rbRecording;

    //declares mad acddress
    public String macAddress="Mac Address Not Found";


    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        //sets the radio button reference
        rbRecording = (RadioButton) findViewById(R.id.rbRecording);

        //sets up the shared preferences file
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);

        //short myLocalThreshold =(short) settings.getInt("threshold", (int) threshold);
        //edTheshold.setText(myLocalThreshold+"");

        int myLocalThreshold =settings.getInt("threshold", threshold);
        threshold=myLocalThreshold;
        edThreshold = (EditText) findViewById(R.id.edThreshold);
        edThreshold.setText( myLocalThreshold +"");

        //threshold = (short) settings.getInt("threshold", (int) threshold);


        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        startAquisition();
        startBreaker();

        startUploader();
        displaySetMac();
        try{
            tvErrors= (TextView) findViewById(R.id.tvErrors);
            //in your OnCreate() method
            tvErrors.setText("-");

            tvBreaker= (TextView) findViewById(R.id.tvBreaker);

            tvLastUpload= (TextView) findViewById(R.id.tvLastUpload);
            //in your OnCreate() method
            tvLastUpload.setText("-");
        }catch (Exception e){
            Toast.makeText(MainActivityBackup8.this, "started", Toast.LENGTH_SHORT).show();
        }


        Toast.makeText(MainActivityBackup8.this, "started", Toast.LENGTH_SHORT).show();
    }


    //updates the threshold value

    public void setThreshold(View v){

        try{

            Toast.makeText(MainActivityBackup8.this, "Setting threshold...", Toast.LENGTH_SHORT).show();

            edThreshold = (EditText) findViewById(R.id.edThreshold);

            int localThreshold = (int) Integer.parseInt(edThreshold.getText().toString());
            if (localThreshold>0){
                threshold =  localThreshold;

                Toast.makeText(MainActivityBackup8.this, "threshold new value is: " + threshold, Toast.LENGTH_SHORT).show();

                SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("threshold", threshold);
                editor.commit();
                Toast.makeText(MainActivityBackup8.this, "set threshold successful", Toast.LENGTH_SHORT).show();

            } else{
                Toast.makeText(MainActivityBackup8.this, "Threshold too large", Toast.LENGTH_SHORT).show();
            }


        }catch( Exception e){
            Toast.makeText(MainActivityBackup8.this, "Problem Setting Threshold", Toast.LENGTH_SHORT).show();
        }

    }

    public void upload(View v){
        Toast.makeText(MainActivityBackup8.this, "triggered upload", Toast.LENGTH_SHORT).show();

        //Toast.makeText(MainActivity.this, "wifi state: " + wifi.isWifiEnabled(), Toast.LENGTH_SHORT).show();
        //Toast.makeText(MainActivity.this, "turned off", Toast.LENGTH_SHORT).show();

        //Uploader muUploader= new Uploader();





        newUploader muUploader = new newUploader();
        try {
            tvErrors.setText("My Uploaded Text");
            Date currentTime = Calendar.getInstance().getTime();
            tvErrors.setText(currentTime +"");

            //muUploader.execute();
            muUploader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Toast.makeText(MainActivityBackup8.this, e+"" , Toast.LENGTH_SHORT).show();

        }

       /* try{
            Connection conn = DriverManager.getConnection("jdbc:mysql:db21.intelligentdemographics.com:3306:db_manni21","mannidbuser","5$fPc_}");

        } catch (Exception e){
            Toast.makeText(MainActivity.this, "couldnt' connecxt :" + e, Toast.LENGTH_SHORT).show();
        }*/

    }



    @Override
    protected void onResume() {
        Log.w(TAG, "onResume");
        super.onResume();


    }

    @Override
    protected void onDestroy() {
        Log.w(TAG, "onDestroy");
        //stopAquisition();
        super.onDestroy();

    }


    // The types specified here are the input data type, the progress type, and the result type
    private class newUploader extends AsyncTask<String, Void, Void> {
        protected void onPreExecute() {
            // Runs on the UI thread before doInBackground
            // Good for toggling visibility of a progress indicator
            Toast.makeText(MainActivityBackup8.this, "pre", Toast.LENGTH_SHORT).show();
        }

        protected Void doInBackground(String... strings) {


            //Toast.makeText(MainActivity.this, "executed", Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {

                public void run() {

                    Toast.makeText(getApplicationContext(), "Example for Toast", Toast.LENGTH_SHORT).show();

                }
            });



            boolean conStatus = false;
            Session session = null;
            Channel channel = null;
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            Log.i("Session","is"+conStatus);
            try {


                String path = Environment.getExternalStorageDirectory().getPath()+"/"+AUDIO_RECORDER_FOLDER;
                //String path = Environment.getExternalStorageDirectory().toString()+"/Pictures";
                Log.d("Files", "Path: " + path);
                File directory = new File(path);
                final File[] files = directory.listFiles();
                Log.d("Files", "Size: "+ files.length);


                //prints out the number of files as a toast
                runOnUiThread(new Runnable() {

                    public void run() {
                        Toast.makeText(getApplicationContext(),  "number of files is: "+files.length, Toast.LENGTH_SHORT).show();

                        //Toast.makeText(getApplicationContext(),  myFiles[i].getName() + "", Toast.LENGTH_SHORT).show();

                    }
                });
             /*
                String path = Environment.getExternalStorageDirectory().toString()+"/Pictures";
                Log.d("Files", "Path: " + path);
                File directory = new File(path);
                File[] files = directory.listFiles();
                Log.d("Files", "Size: "+ files.length);*/

                for (int i = 0; i < files.length; i++)
                {
                    Log.d("Files", "FileName:" + files[i].getName());
                }



                // If there are files to upload we will enter the upload loop. If there is only one file, then don't since that is the temp file.
                if (files.length>1){

                    // Some long-running task like downloading an image.
                    wifi.setWifiEnabled(true); // true or false to activate/deactivate wifi

                    //waiting for a specific period of time to ensure that we have connected before continuint with the thread
                   /* try {
                        Thread.sleep(13000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/

                    //waiting for as long as the wifi is not connected, with a maximum wait time of the Wifi sec variable set at the beginning.
                    try{

                        int loopsLeft=0;

                        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                        while(!mWifi.isConnected() && loopsLeft<WIFI_SECS){
                            loopsLeft++;
                            Thread.sleep(1000);

                        }


                    } catch(Exception e){
                        Log.d("Wifi Wait Exception", "Wifi wait thread had a problem");
                    }



                    JSch ssh = new JSch();
                    session = ssh.getSession("root", "45.76.166.207", 22);
                    session.setPassword("5$fPc_}*8gTo-V6#");

                    session.setConfig(config);

                    session.connect();

                    conStatus = session.isConnected();
                    Log.i("Session","is"+conStatus);

                    channel = session.openChannel("sftp");
                    channel.connect();
                    ChannelSftp sftp = (ChannelSftp) channel;



                    for (int i = 0; i < files.length; i++)
                    {
                        if (!files[i].getName().equals(AUDIO_RECORDER_TEMP_FILE)) {
                            sftp.put(path+"/"+files[i].getName(), "/root/"+macAddress+"/");

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    Date currentTime = Calendar.getInstance().getTime();
                                    tvLastUpload.setText(currentTime +"");

                                }
                            });



                            files[i].delete();
                        }


                    }


                    runOnUiThread(new Runnable() {

                        public void run() {
                            Toast.makeText(getApplicationContext(),  "deleted them all ", Toast.LENGTH_SHORT).show();

                            //Toast.makeText(getApplicationContext(),  myFiles[i].getName() + "", Toast.LENGTH_SHORT).show();

                        }
                    });
                }




            } catch (final Exception e) {
                // TODO Auto-generated catch block
                runOnUiThread(new Runnable() {

                    public void run() {

                        Toast.makeText(getApplicationContext(), "this is the exception: " + e, Toast.LENGTH_SHORT).show();

                    }
                });

               if(e.toString().equals("4: Failure")){
                   runOnUiThread(new Runnable() {

                       public void run() {

                           Toast.makeText(getApplicationContext(), "Directory Not Found, Creating New", Toast.LENGTH_SHORT).show();

                       }

                   });

                   try{
                       //creates a whole new session in order to make the folder
                       JSch ssh = new JSch();
                       session = ssh.getSession("root", "45.76.166.207", 22);
                       session.setPassword("5$fPc_}*8gTo-V6#");

                       session.setConfig(config);

                       session.connect();

                       conStatus = session.isConnected();
                       Log.i("Session","is"+conStatus);

                       channel = session.openChannel("sftp");
                       channel.connect();
                       ChannelSftp sftp = (ChannelSftp) channel;
                       sftp.mkdir("/root/"+macAddress+"/");

                   }catch (Exception d){
                       Log.i("Session","is"+conStatus);
                   }

                   runOnUiThread(new Runnable() {

                       public void run() {

                           Toast.makeText(getApplicationContext(), "Created Directory with Given Mac", Toast.LENGTH_SHORT).show();

                       }

                   });




               }
                Log.i("Session","is"+conStatus);
            } finally{
                try{
                    wifi.setWifiEnabled(false); // true or false to activate/deactivate wifi

                }catch(Exception e){
                    Log.i("Session","is"+conStatus);
                }

            }




            return null;
        }



        protected void onPostExecute(Bitmap result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            Toast.makeText(MainActivityBackup8.this, "post", Toast.LENGTH_SHORT).show();
        }
    }


    public class RecordAudio extends AsyncTask<Void, Double, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            Log.w(TAG, "doInBackground");
            try {

                String filename = getTempFilename();

                try {
                    os = new FileOutputStream(filename);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }


                bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);

                AudioRecord audioRecord = new AudioRecord( MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[bufferSize];

                audioRecord.startRecording();
                int silence= SILENCE_LOOPS+1;
                while (started) {

                    try{


                        int bufferReadResult = audioRecord.read(buffer, 0,bufferSize);
                        if(AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult){


                            //check signal
                            //put a threshold

                            int foundPeak=searchThreshold(buffer,threshold);
                            if (foundPeak>-1){ //found signal
                                if(silence>=SILENCE_LOOPS){
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {

                                            rbRecording.setChecked(true);

                                        }
                                    });

                                }
                                silence=0;
                                //record signal
                                byte[] byteBuffer =ShortToByte(buffer,bufferReadResult);
                                try {
                                    os.write(byteBuffer);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                silence++;
                                if (silence<SILENCE_LOOPS){
                                    //record signal
                                    byte[] byteBuffer =ShortToByte(buffer,bufferReadResult);
                                    try {
                                        os.write(byteBuffer);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }else if (silence==SILENCE_LOOPS){
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {

                                            //rbRecording.toggle();
                                            //rbRecording.isChecked();
                                            rbRecording.setChecked(false);

                                        }
                                    });

                                }

                            }


                            //show results
                            //here, with publichProgress function, if you calculate the total saved samples,
                            //you can optionally show the recorded file length in seconds:      publishProgress(elsapsedTime,0);


                        }

                    } catch (final Exception e){


                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                tvErrors= (TextView) findViewById(R.id.tvErrors);
                                //in your OnCreate() method
                                tvErrors.setText("Exception: " +e);

                            }
                        });


                    }


                }

                audioRecord.stop();


                //close file
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                copyWaveFile(getTempFilename(),getFilename());
                deleteTempFile();


            } catch (Throwable t) {
                t.printStackTrace();
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;

        } //fine di doInBackground

        byte [] ShortToByte(short [] input, int elements) {
            int short_index, byte_index;
            int iterations = elements; //input.length;
            byte [] buffer = new byte[iterations * 2];

            short_index = byte_index = 0;

            for(/*NOP*/; short_index != iterations; /*NOP*/)
            {
                buffer[byte_index]     = (byte) (input[short_index] & 0x00FF);
                buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);

                ++short_index; byte_index += 2;
            }

            return buffer;
        }


        int searchThreshold(short[]arr, int thr){
            int peakIndex;
            int arrLen=arr.length;
            for (peakIndex=0;peakIndex<arrLen;peakIndex++){
                if ((arr[peakIndex]>=thr) || (arr[peakIndex]<=-thr)){
                    //se supera la soglia, esci e ritorna peakindex-mezzo kernel.

                    return peakIndex;
                }
            }
            return -1; //not found
        }

    /*
    @Override
    protected void onProgressUpdate(Double... values) {
        DecimalFormat sf = new DecimalFormat("000.0000");
        elapsedTimeTxt.setText(sf.format(values[0]));

    }
    */

        private String getFilename(){
            String filepath = Environment.getExternalStorageDirectory().getPath();
            File file = new File(filepath,AUDIO_RECORDER_FOLDER);

            if(!file.exists()){
                file.mkdirs();
            }

            return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
        }


        private String getTempFilename(){
            String filepath = Environment.getExternalStorageDirectory().getPath();
            File file = new File(filepath,AUDIO_RECORDER_FOLDER);

            if(!file.exists()){
                file.mkdirs();
            }

            File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

            if(tempFile.exists())
                tempFile.delete();

            return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
        }





        private void deleteTempFile() {
            File file = new File(getTempFilename());

            file.delete();

        }

        private void clearTempFile() {

             try{
                        String filename = getTempFilename();
                        os.close();
                        File file = new File(filename);

                        file.delete();

                        try {
                            os = new FileOutputStream(filename);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
/*

                os.flush();
                new PrintWriter(getTempFilename()).close();*/
                //assertEquals(0, StreamUtils.getStringFromInputStream(new FileInputStream(FILE_PATH)).length());


            } catch(Exception e){



            }




        }

        private void copyWaveFile(String inFilename,String outFilename){
            FileInputStream in = null;
            FileOutputStream out = null;
            long totalAudioLen = 0;
            long totalDataLen = totalAudioLen + 36;
            long longSampleRate = frequency;
            int channels = 1;
            long byteRate = RECORDER_BPP * frequency * channels/8;

            byte[] data = new byte[bufferSize];

            try {
                in = new FileInputStream(inFilename);
                out = new FileOutputStream(outFilename);
                totalAudioLen = in.getChannel().size();
                totalDataLen = totalAudioLen + 36;


                WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                        longSampleRate, channels, byteRate);

                while(in.read(data) != -1){
                    out.write(data);
                }

                in.close();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //atteempt to clear the file without messing up the file size.
            try{
                String filename = getTempFilename();
                os.close();
                File file = new File(filename);

                file.delete();

                try {
                    os = new FileOutputStream(filename);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
/*

                os.flush();
                new PrintWriter(getTempFilename()).close();*/
                //assertEquals(0, StreamUtils.getStringFromInputStream(new FileInputStream(FILE_PATH)).length());


            } catch(Exception e){



            }
        }

        private void WriteWaveFileHeader(
                FileOutputStream out, long totalAudioLen,
                long totalDataLen, long longSampleRate, int channels,
                long byteRate) throws IOException {

            byte[] header = new byte[44];

            header[0] = 'R';  // RIFF/WAVE header
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f';  // 'fmt ' chunk
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1;  // format = 1
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) (longSampleRate & 0xff);
            header[25] = (byte) ((longSampleRate >> 8) & 0xff);
            header[26] = (byte) ((longSampleRate >> 16) & 0xff);
            header[27] = (byte) ((longSampleRate >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (channels * 16 / 8);  // block align
            header[33] = 0;
            header[34] = RECORDER_BPP;  // bits per sample
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

            out.write(header, 0, 44);
        }

    } //Fine Classe RecordAudio (AsyncTask)

  /*  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main,
                menu);
        return true;

    }*/


    public void resetAquisition() {
        Log.w(TAG, "resetAquisition");
        stopAquisition();
        //startButton.setText("WAIT");
        startAquisition();
    }

    public void stopAquisition() {
        Log.w(TAG, "stopAquisition");
        if (started) {
            started = false;
            recordTask.cancel(true);
        }
    }

    public void displaySetMac() {

        // Some long-running task like downloading an image.
        wifi.setWifiEnabled(true); // true or false to activate/deactivate wifi

        //waiting for a specific period of time to ensure that we have connected before continuint with the thread
        try {
            Thread.sleep(13000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.w(TAG, "stopDisplayMac");
        TextView tvMac= (TextView) findViewById(R.id.tvMac);


        //WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        macAddress = info.getMacAddress();

        tvMac.setText(macAddress +"");

        // Some long-running task like downloading an image.
        wifi.setWifiEnabled(false); // true or false to activate/deactivate wifi


    }

    public void startAquisition(){
        Log.w(TAG, "startAquisition");
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {

                //elapsedTime=0;
                started = true;
                recordTask = new RecordAudio();
                recordTask.execute();
                //startButton.setText("RESET");
            }
        }, 500);
    }



    public void LoadDriver(){
        try {
            // The newInstance() call is a work around for some
            // broken Java implementations

            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            // handle the error
        }
    }


    public void startBreaker(){
        Log.w(TAG, "startBreaker");
        final Handler breakerHandler = new Handler();
        final int delay = 90000;
        breakerHandler.postDelayed(new Runnable() {
            public void run() {



                try{
                   /* Toast.makeText(MainActivity.this, "1", Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "2", Toast.LENGTH_SHORT).show();*/

                    breakerHandler.postDelayed(this, delay);
                    File f = new File(recordTask.getTempFilename());

                    Toast.makeText(MainActivityBackup8.this, "2length is "+ f.length(), Toast.LENGTH_SHORT).show();
                    if  (f.length()>2000000){
                        recordTask.copyWaveFile(recordTask.getTempFilename(),recordTask.getFilename());
                        //recordTask.clearTempFile();
                    }

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            Date currentTime = Calendar.getInstance().getTime();
                            tvBreaker.setText(currentTime +"");

                        }
                    });

                } catch (Exception e){
                    Toast.makeText(MainActivityBackup8.this, "Exception with the startbreaker", Toast.LENGTH_SHORT).show();
                }


                //elapsedTime=0;
               /* int myNum=1;
                while (true){
                    myNum++;
                    SystemClock.sleep(7000);
                    //Toast.makeText(MainActivity.this, myNum + "", Toast.LENGTH_SHORT).show();
                }*/
            }
        }, delay);
    }


    public void startUploader(){
        Log.w(TAG, "startBreaker");
        final Handler breakerHandler = new Handler();
        final int delay2 = 300000;
        breakerHandler.postDelayed(new Runnable() {
            public void run() {



                try{
                   /* Toast.makeText(MainActivity.this, "1", Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "2", Toast.LENGTH_SHORT).show();*/

                    breakerHandler.postDelayed(this, delay2);



                    try {
                        Toast.makeText(MainActivityBackup8.this, "triggered upload", Toast.LENGTH_SHORT).show();

                        //Toast.makeText(MainActivity.this, "wifi state: " + wifi.isWifiEnabled(), Toast.LENGTH_SHORT).show();
                        //Toast.makeText(MainActivity.this, "turned off", Toast.LENGTH_SHORT).show();

                        //Uploader muUploader= new Uploader();
                        newUploader muUploader = new newUploader();
                        //muUploader.execute();
                        muUploader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        Toast.makeText(MainActivityBackup8.this, e+"" , Toast.LENGTH_SHORT).show();

                    }

                } catch (Exception e){
                    Toast.makeText(MainActivityBackup8.this, "Exception with the startbreaker", Toast.LENGTH_SHORT).show();
                }


                //elapsedTime=0;
               /* int myNum=1;
                while (true){
                    myNum++;
                    SystemClock.sleep(7000);
                    //Toast.makeText(MainActivity.this, myNum + "", Toast.LENGTH_SHORT).show();
                }*/
            }
        }, delay2);
    }


}