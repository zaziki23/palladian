package tud.iir.news.evaluation;

import java.sql.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;

public class ChartCreator {

    private static final Logger LOGGER = Logger.getLogger(ChartCreator.class);
    
    private final String FEED_SIZE_HISTOGRAM_FILE_PATH = "data/evaluation/feedSizeHistogrammData.csv";
    private final String feedAgeFilePath = "data/evaluation/feedAgeData.csv";
    private final String timeliness2FilePath = "data/evaluation/timeliness2Data.csv";
    private final String percentageNewFilePath = "data/evaluation/percentNewData.csv";
    private final String sumVolumeMaxFilePath = "data/evaluation/sumVolumeMaxData.csv";
    private final String sumVolumeMinFilePath = "data/evaluation/sumVolumeMinData.csv";
    
    
    private final int MAX_NUMBER_OF_POLLS_SCORE_MIN = 1000;
    private final int MAX_NUMBER_OF_POLLS_SCORE_MAX = 1000;
    
    private void createFeedSizeHistogrammFile(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        List<EvaluationFeedPoll> polls = ed.getFeedSizes();
        int[] feedSizeDistribution = new int[21];
        
        for (EvaluationFeedPoll poll : polls) {
//            int feedID = poll.getFeedID();
//            float pollSize = poll.getSizeOfPoll();
            int i =  new Double(Math.floor(poll.getSizeOfPoll()/10/1024)).intValue() ;
            i = (i > 20)? 20 : i;
            feedSizeDistribution[i]++;             
        }        
        
        StringBuilder feedSizeDistributionSB = new StringBuilder();
        feedSizeDistributionSB.append("Feed size in KB;all feeds;\n");
        int size = 0;
        String size2 = "";
        for (int number : feedSizeDistribution) {
            size +=10;
            size2 = (size > 200)? "more" : String.valueOf(size);
            feedSizeDistributionSB.append(size2).append(";").append(number).append(";\n");                        
        }
        
        FileHelper.writeToFile(FEED_SIZE_HISTOGRAM_FILE_PATH, feedSizeDistributionSB);
        LOGGER.info("feedSizeHistogrammFile *hopefully* :) written to: " + FEED_SIZE_HISTOGRAM_FILE_PATH);
    }
    
    
    private void createFeedAgeFile(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        List<EvaluationItemIntervalItem> polls = ed.getAverageUpdateIntervals();
        int[] feedAgeDistribution = new int[34];
        
        for (EvaluationItemIntervalItem intervalItem : polls) {
            int averageUpdateIntervalHours = new Double(Math.floor(intervalItem.getAverageUpdateInterval()/3600000)).intValue();
            int i = -1;
            if(averageUpdateIntervalHours <= 24) i = averageUpdateIntervalHours;
            else if(averageUpdateIntervalHours <= 24*2) i = 24;  //2 days
            else if(averageUpdateIntervalHours <= 24*3) i = 25;  //3 days
            else if(averageUpdateIntervalHours <= 24*4) i = 26;  //4 days
            else if(averageUpdateIntervalHours <= 24*5) i = 27; //5 days
            else if(averageUpdateIntervalHours <= 24*6) i = 28; //6 days
            else if(averageUpdateIntervalHours <= 24*7) i = 29; //7 days
            else if(averageUpdateIntervalHours <= 24*7*2) i = 30; //2 weeks
            else if(averageUpdateIntervalHours <= 24*7*3) i = 31; //3 weeks
            else if(averageUpdateIntervalHours <= 24*7*4) i = 32; //4 weeks
            else i = 33; //more
            feedAgeDistribution[i]++;             
        }        

        StringBuilder feedAgeSB = new StringBuilder();
        feedAgeSB.append("Feed age;all feeds;\n");
        int i = 0;
        String[] caption = {"1 hour","2 hours","3 hours","4 hours","5 hours","6 hours","7 hours","8 hours","9 hours","10 hours","11 hours","12 hours","13 hours","14 hours","15 hours","16 hours","17 hours","18 hours","19 hours","20 hours","21 hours","22 hours","23 hours","24 hours","2 days","3 days","4 days","5 days","6 days","7 days","2 weeks","3 weeks","4 weeks","more"};
        
        for (int number : feedAgeDistribution) {
            feedAgeSB.append(caption[i]).append(";").append(number).append(";\n");
            i++;
        }
        
        FileHelper.writeToFile(feedAgeFilePath, feedAgeSB);
        LOGGER.info("feedAgeFile *hopefully* :) written to: " + feedAgeFilePath);       
    }
    

    private void createTimeliness2File(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        StringBuilder timeliness2SB = new StringBuilder();        
        Map<Integer,Double[]> testMap = new TreeMap<Integer,Double[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        timeliness2SB.append("numberOfPoll;");
        
        timeliness2SB.append("fix1440;");
        polls = ed.getAverageScoreMinFIX1440(MAX_NUMBER_OF_POLLS_SCORE_MIN);
        for (EvaluationFeedPoll poll : polls) {            
            Double[] testDouble = new Double[6];
            testDouble[0] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }
        
        timeliness2SB.append("fix720;");
        polls = ed.getAverageScoreMinFIX720(MAX_NUMBER_OF_POLLS_SCORE_MIN);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[1] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }
        
        timeliness2SB.append("fix60;");
        polls = ed.getAverageScoreMinFIX60(MAX_NUMBER_OF_POLLS_SCORE_MIN);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[2] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }        

        timeliness2SB.append("fixLearned;");
        polls = ed.getAverageScoreMinFIXlearned(MAX_NUMBER_OF_POLLS_SCORE_MIN);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[3] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }
        
        timeliness2SB.append("adaptive;");
        polls = ed.getAverageScoreMinAdaptive(MAX_NUMBER_OF_POLLS_SCORE_MIN);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[4] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }

        timeliness2SB.append("probabilistic;\n");
        polls = ed.getAverageScoreMinProbabilistic(MAX_NUMBER_OF_POLLS_SCORE_MIN);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[5] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }        
        
        int i = 1;
        for(Double[] scores : testMap.values()){
            timeliness2SB.append(i).append(";").append(scores[0]).append(";").append(scores[1]).append(";").append(scores[2]).append(";").append(scores[3]).append(";").append(scores[4]).append(";").append(scores[5]).append(";\n");
            i++;            
        }
        
        FileHelper.writeToFile(timeliness2FilePath, timeliness2SB);
        LOGGER.info("timeliness2File *hopefully* :) written to: " + timeliness2FilePath);
    } 
    
    
    


    private void createPercentageNewFile(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        StringBuilder timeliness2SB = new StringBuilder();        
        Map<Integer,Double[]> testMap = new TreeMap<Integer,Double[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        timeliness2SB.append("numberOfPoll;");
        
        timeliness2SB.append("fix1440;");
        polls = ed.getAverageScoreMaxFIX1440(MAX_NUMBER_OF_POLLS_SCORE_MAX);
        for (EvaluationFeedPoll poll : polls) {            
            Double[] testDouble = new Double[6];
            testDouble[0] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }
        
        timeliness2SB.append("fix720;");
        polls = ed.getAverageScoreMaxFIX720(MAX_NUMBER_OF_POLLS_SCORE_MAX);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[1] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }
        
        timeliness2SB.append("fix60;");
        polls = ed.getAverageScoreMaxFIX60(MAX_NUMBER_OF_POLLS_SCORE_MAX);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[2] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }        

        timeliness2SB.append("fixLearned;");
        polls = ed.getAverageScoreMaxFIXlearned(MAX_NUMBER_OF_POLLS_SCORE_MAX);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[3] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }
        
        timeliness2SB.append("adaptive;");
        polls = ed.getAverageScoreMaxAdaptive(MAX_NUMBER_OF_POLLS_SCORE_MAX);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[4] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }

        timeliness2SB.append("probabilistic;\n");
        polls = ed.getAverageScoreMaxProbabilistic(MAX_NUMBER_OF_POLLS_SCORE_MAX);                
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] testDouble = new Double[6];
            if( testMap.containsKey(pollToProcess)){
                testDouble = testMap.get(pollToProcess);
            }
            testDouble[5] = poll.getScoreAVG();
            testMap.put(poll.getNumberOfPoll(), testDouble);
        }        
        
        int i = 1;
        for(Double[] scores : testMap.values()){
            timeliness2SB.append(i).append(";").append(scores[0]).append(";").append(scores[1]).append(";").append(scores[2]).append(";").append(scores[3]).append(";").append(scores[4]).append(";").append(scores[5]).append(";\n");
            i++;            
        }
        
        FileHelper.writeToFile(percentageNewFilePath, timeliness2SB);
        LOGGER.info("percentageNewFile *hopefully* :) written to: " + percentageNewFilePath);
    } 
    
    
    
    
    
    
    
    private void culmulatedVolumeMaxTimeFile(){
        LOGGER.info("starting to create sumVolumeMaxFile...");
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        StringBuilder culmulatedVolumeSB = new StringBuilder();
        /* <hourOfExperiment, culmulatedVolumePerAlgorithm in Bytes>  */
        Map<Integer,Long[]> totalResultMapMax = new TreeMap<Integer,Long[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        int totalExperimentHours = 672;
        long culmulatedVolumePerTechnique = 0;
        int feedIDLastStep = -1;
        int requiredNumberOfPolls = -1; 
        float sizeOfPollLast = -1;
        int hourLastStep = -1;
        int numberOfPollsToProcess = -1;
        int pollingInterval = -1;
        
        culmulatedVolumeSB.append("hour of experiment;");
    

        
        ///////////// get data from fix1440Time \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fix1440 data...");
        culmulatedVolumeSB.append("fix1440;");
        culmulatedVolumePerTechnique = 0;
        feedIDLastStep = -1;
        pollingInterval = 24;
        requiredNumberOfPolls = totalExperimentHours/pollingInterval;        
        polls = ed.getSumTransferVolumeByHourFromFix1440MaxMinTime();       
        for (EvaluationFeedPoll poll : polls) {
            Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            int feedIDCurrent = poll.getFeedID();
            
            // in der DB nicht vorhandene Polls generieren 
            if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                while (numberOfPollsToProcess > 0){
                    numberOfPollsToProcess--;
                    int hourToProcess = hourLastStep + pollingInterval;
                    long culmulatedVolumePerHour = 0;
                    transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                    
                    if( totalResultMapMax.containsKey(hourToProcess)){
                        transferredDataArray = totalResultMapMax.get(hourToProcess);
                        if (transferredDataArray[0] != null) {
                            culmulatedVolumePerHour = transferredDataArray[0];
                        }                        
                    }
                    culmulatedVolumePerHour += sizeOfPollLast;
                    transferredDataArray[0] = culmulatedVolumePerHour;                                        
                    totalResultMapMax.put(hourToProcess, transferredDataArray);
                    hourLastStep = hourToProcess;
                }                
                // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
                numberOfPollsToProcess = requiredNumberOfPolls;
            }
                   
            // aktuellen Poll behandeln
            int hourToProcess = poll.getHourOfExperiment();
            long culmulatedVolumePerHour = 0;
            transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            
            if( totalResultMapMax.containsKey(hourToProcess)){
                transferredDataArray = totalResultMapMax.get(hourToProcess);
                if (transferredDataArray[0] != null) {
                    culmulatedVolumePerHour = transferredDataArray[0];
                }                        
            }
            float sizeOfPoll = poll.getSizeOfPoll();
            culmulatedVolumePerHour += sizeOfPoll;
            transferredDataArray[0] = culmulatedVolumePerHour;                                        
            totalResultMapMax.put(hourToProcess, transferredDataArray);
            
            numberOfPollsToProcess--;
            hourLastStep = hourToProcess;
            feedIDLastStep = feedIDCurrent;
            sizeOfPollLast = sizeOfPoll;
        }
        LOGGER.info("finished creating fix1440 data...");
        
        

        ///////////// get data from fix720Time \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fix720 data...");
        culmulatedVolumeSB.append("fix720;");
        culmulatedVolumePerTechnique = 0;
        feedIDLastStep = -1;
        pollingInterval = 12;
        requiredNumberOfPolls = totalExperimentHours/pollingInterval;
        
        polls = ed.getSumTransferVolumeByHourFromFix720MaxMinTime();
       
        for (EvaluationFeedPoll poll : polls) {
            Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            int feedIDCurrent = poll.getFeedID();
            
            // in der DB nicht vorhandene Polls generieren 
            if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                while (numberOfPollsToProcess > 0){
                    numberOfPollsToProcess--;
                    int hourToProcess = hourLastStep + pollingInterval;
                    long culmulatedVolumePerHour = 0;
                    transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                    
                    if( totalResultMapMax.containsKey(hourToProcess)){
                        transferredDataArray = totalResultMapMax.get(hourToProcess);
                        if (transferredDataArray[1] != null) {
                            culmulatedVolumePerHour = transferredDataArray[1];
                        }                        
                    }
                    culmulatedVolumePerHour += sizeOfPollLast;
                    transferredDataArray[1] = culmulatedVolumePerHour;                                        
                    totalResultMapMax.put(hourToProcess, transferredDataArray);
                    hourLastStep = hourToProcess;
                }                
                // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
                numberOfPollsToProcess = requiredNumberOfPolls;
            }
                   
            // aktuellen Poll behandeln
            int hourToProcess = poll.getHourOfExperiment();
            long culmulatedVolumePerHour = 0;
            transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            
            if( totalResultMapMax.containsKey(hourToProcess)){
                transferredDataArray = totalResultMapMax.get(hourToProcess);
                if (transferredDataArray[1] != null) {
                    culmulatedVolumePerHour = transferredDataArray[1];
                }                        
            }
            float sizeOfPoll = poll.getSizeOfPoll();
            culmulatedVolumePerHour += sizeOfPoll;
            transferredDataArray[1] = culmulatedVolumePerHour;                                        
            totalResultMapMax.put(hourToProcess, transferredDataArray);
            
            numberOfPollsToProcess--;
            hourLastStep = hourToProcess;
            feedIDLastStep = feedIDCurrent;
            sizeOfPollLast = sizeOfPoll;
        }
        LOGGER.info("finished creating fix720 data...");
        

        ///////////// get data from fix60Time \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fix60 data...");
        culmulatedVolumeSB.append("fix60;");
        culmulatedVolumePerTechnique = 0;
        feedIDLastStep = -1;
        pollingInterval = 1;
        requiredNumberOfPolls = totalExperimentHours/pollingInterval;
        
        int feedIDStart = 1;
        int feedIDEnd = 10000;
        final int FEED_ID_STEP = 10000;
        final int FEED_ID_MAX = 210000;
        
        while(feedIDEnd < FEED_ID_MAX){
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
            polls = ed.getSumTransferVolumeByHourFromFix60MaxMinTime(feedIDStart, feedIDEnd);
            numberOfPollsToProcess = requiredNumberOfPolls;
            
            for (EvaluationFeedPoll poll : polls) {
                Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                int feedIDCurrent = poll.getFeedID();
                
                // in der DB nicht vorhandene Polls generieren 
                if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                    while (numberOfPollsToProcess > 0){
                        int hourToProcess = hourLastStep + pollingInterval;
                        long culmulatedVolumePerHour = 0;
                        transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                        
                        if( totalResultMapMax.containsKey(hourToProcess)){
                            transferredDataArray = totalResultMapMax.get(hourToProcess);
                            if (transferredDataArray[2] != null) {
                                culmulatedVolumePerHour = transferredDataArray[2];
                            }                        
                        }
                        culmulatedVolumePerHour += sizeOfPollLast;
                        transferredDataArray[2] = culmulatedVolumePerHour;                                        
                        totalResultMapMax.put(hourToProcess, transferredDataArray);
                        hourLastStep = hourToProcess;
                        numberOfPollsToProcess--;
                    }                
                    // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
                    numberOfPollsToProcess = requiredNumberOfPolls;
                }
                       
                // aktuellen Poll behandeln
                int hourToProcess = poll.getHourOfExperiment();
                long culmulatedVolumePerHour = 0;
                transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                
                if( totalResultMapMax.containsKey(hourToProcess)){
                    transferredDataArray = totalResultMapMax.get(hourToProcess);
                    if (transferredDataArray[2] != null) {
                        culmulatedVolumePerHour = transferredDataArray[2];
                    }                        
                }
                float sizeOfPoll = poll.getSizeOfPoll();
                culmulatedVolumePerHour += sizeOfPoll;
                transferredDataArray[2] = culmulatedVolumePerHour;                                        
                totalResultMapMax.put(hourToProcess, transferredDataArray);
                
                numberOfPollsToProcess--;
                hourLastStep = hourToProcess;
                feedIDLastStep = feedIDCurrent;
                sizeOfPollLast = sizeOfPoll;
            }
            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating fix60 data...");
        
        
        ///////////// get data from fixLearnedTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fixLearned data...");
        culmulatedVolumeSB.append("fixLearned;");
        culmulatedVolumePerTechnique = 0;
        feedIDLastStep = -1;
        numberOfPollsToProcess = -1;
        feedIDStart = 1;
        feedIDEnd = 10000;
        
        while(feedIDEnd < FEED_ID_MAX){
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);            
            int minuteLastStep = 0;                        
            
            polls = ed.getSumTransferVolumeByHourFromFixLearnedMaxTime(feedIDStart, feedIDEnd);
            
            for (EvaluationFeedPoll poll : polls) {
                Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                int feedIDCurrent = poll.getFeedID();
                
                // in der DB nicht vorhandene Polls generieren 
                if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                    while (minuteLastStep < totalExperimentHours*60){
                        
                        int minuteToProcess = (int)(minuteLastStep + poll.getCheckInterval());
                        int hourToProcess = minuteToProcess/60;
                        long culmulatedVolumePerHour = 0;
                        transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                        
                        if( totalResultMapMax.containsKey(hourToProcess)){
                            transferredDataArray = totalResultMapMax.get(hourToProcess);
                            if (transferredDataArray[3] != null) {
                                culmulatedVolumePerHour = transferredDataArray[3];
                            }                        
                        }
                        culmulatedVolumePerHour += sizeOfPollLast;
                        transferredDataArray[3] = culmulatedVolumePerHour;                                        
                        totalResultMapMax.put(hourToProcess, transferredDataArray);                        
                        minuteLastStep = minuteToProcess;
                        numberOfPollsToProcess--;
                    }
                }                      
                
                // aktuellen Poll behandeln                
                int hourToProcess = poll.getHourOfExperiment();
                long culmulatedVolumePerHour = 0;
                transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                
                if( totalResultMapMax.containsKey(hourToProcess)){
                    transferredDataArray = totalResultMapMax.get(hourToProcess);
                    if (transferredDataArray[3] != null) {
                        culmulatedVolumePerHour = transferredDataArray[3];
                    }                        
                }
                float sizeOfPoll = poll.getSizeOfPoll();
                culmulatedVolumePerHour += sizeOfPoll;
                transferredDataArray[3] = culmulatedVolumePerHour;                                        
                totalResultMapMax.put(hourToProcess, transferredDataArray);
                
                if(poll.getNumberOfPoll() > 2) {
                    minuteLastStep += (int)poll.getCheckInterval();
                }
                feedIDLastStep = feedIDCurrent;
                sizeOfPollLast = sizeOfPoll;
            }
            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating fixLearned data...");
        
                
        
        ///////////// get data from adaptiveMaxTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create adaptive data...");
        culmulatedVolumeSB.append("adaptive;");
        culmulatedVolumePerTechnique = 0;
        polls = ed.getSumTransferVolumeByHourFromAdaptiveMaxTime();
        for (EvaluationFeedPoll poll : polls) {
            Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            int hourToProcess = poll.getHourOfExperiment();            
            if( totalResultMapMax.containsKey(hourToProcess)){
                transferredDataArray = totalResultMapMax.get(hourToProcess);
            }
            
            culmulatedVolumePerTechnique += poll.getCulmulatedSizeofPolls();
            transferredDataArray[4] = culmulatedVolumePerTechnique;
            totalResultMapMax.put(hourToProcess, transferredDataArray);
        }
        LOGGER.info("finished creating adaptive data...");

        
        ///////////// get data from probabilisticMaxTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create probabilistic data...");
        culmulatedVolumeSB.append("probabilistic;\n");
        culmulatedVolumePerTechnique = 0;
        polls = ed.getSumTransferVolumeByHourFromProbabilisticMaxTime();
        for (EvaluationFeedPoll poll : polls) {
            Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            int hourToProcess = poll.getHourOfExperiment();            
            if( totalResultMapMax.containsKey(hourToProcess)){
                transferredDataArray = totalResultMapMax.get(hourToProcess);
            }
            
            culmulatedVolumePerTechnique += poll.getCulmulatedSizeofPolls();
            transferredDataArray[5] = culmulatedVolumePerTechnique;
            totalResultMapMax.put(hourToProcess, transferredDataArray);
        }
        LOGGER.info("finished creating probabilistic data...");

                
        int i = 1;
        final long BYTE_TO_MB = 1048576;
        Long[] volumesCumulated = new Long[]{0l,0l,0l,0l,0l,0l};
        for(Long[] volumes : totalResultMapMax.values()){
            volumesCumulated[0] += volumes[0];
            volumesCumulated[1] += volumes[1];
            volumesCumulated[2] += volumes[2];
            volumesCumulated[3] += volumes[3];
            culmulatedVolumeSB.append(i).append(";")
                .append(volumesCumulated[0]/BYTE_TO_MB).append(";")
                .append(volumesCumulated[1]/BYTE_TO_MB).append(";")
                .append(volumesCumulated[2]/BYTE_TO_MB).append(";")
                .append(volumesCumulated[3]/BYTE_TO_MB).append(";")
                .append(volumes[4]/BYTE_TO_MB).append(";")
                .append(volumes[5]/BYTE_TO_MB).append(";\n");            
            i++;   
        }
        
        FileHelper.writeToFile(sumVolumeMaxFilePath, culmulatedVolumeSB);
        LOGGER.info("sumVolumeMaxFilePath *hopefully* :) written to: " + sumVolumeMaxFilePath);
    } 
    
    
    
    
    private void culmulatedVolumeMinTimeFile(){
        LOGGER.info("starting to create sumVolumeMinFile...");
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        StringBuilder culmulatedVolumeSB = new StringBuilder();
        /* <hourOfExperiment, culmulatedVolumePerAlgorithm in Bytes>  */
        Map<Integer,Long[]> totalResultMapMin = new TreeMap<Integer,Long[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        int totalExperimentHours = 672;
        long culmulatedVolumePerTechnique = 0;
        int feedIDLastStep = -1;
        int requiredNumberOfPolls = -1; 
        float sizeOfPollLast = -1;
        int hourLastStep = -1;
        int numberOfPollsToProcess = -1;
        int pollingInterval = -1;
        
        culmulatedVolumeSB.append("hour of experiment;");
    

        
        ///////////// get data from fix1440Time \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fix1440 data...");
        culmulatedVolumeSB.append("fix1440;");
        culmulatedVolumePerTechnique = 0;
        feedIDLastStep = -1;
        pollingInterval = 24;
        requiredNumberOfPolls = totalExperimentHours/pollingInterval;        
        polls = ed.getSumTransferVolumeByHourFromFix1440MaxMinTime();       
        for (EvaluationFeedPoll poll : polls) {
            Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            int feedIDCurrent = poll.getFeedID();
            
            // in der DB nicht vorhandene Polls generieren 
            if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                while (numberOfPollsToProcess > 0){
                    numberOfPollsToProcess--;
                    int hourToProcess = hourLastStep + pollingInterval;
                    long culmulatedVolumePerHour = 0;
                    transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                    
                    if( totalResultMapMin.containsKey(hourToProcess)){
                        transferredDataArray = totalResultMapMin.get(hourToProcess);
                        if (transferredDataArray[0] != null) {
                            culmulatedVolumePerHour = transferredDataArray[0];
                        }                        
                    }
                    culmulatedVolumePerHour += sizeOfPollLast;
                    transferredDataArray[0] = culmulatedVolumePerHour;                                        
                    totalResultMapMin.put(hourToProcess, transferredDataArray);
                    hourLastStep = hourToProcess;
                }                
                // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
                numberOfPollsToProcess = requiredNumberOfPolls;
            }
                   
            // aktuellen Poll behandeln
            int hourToProcess = poll.getHourOfExperiment();
            long culmulatedVolumePerHour = 0;
            transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            
            if( totalResultMapMin.containsKey(hourToProcess)){
                transferredDataArray = totalResultMapMin.get(hourToProcess);
                if (transferredDataArray[0] != null) {
                    culmulatedVolumePerHour = transferredDataArray[0];
                }                        
            }
            float sizeOfPoll = poll.getSizeOfPoll();
            culmulatedVolumePerHour += sizeOfPoll;
            transferredDataArray[0] = culmulatedVolumePerHour;                                        
            totalResultMapMin.put(hourToProcess, transferredDataArray);
            
            numberOfPollsToProcess--;
            hourLastStep = hourToProcess;
            feedIDLastStep = feedIDCurrent;
            sizeOfPollLast = sizeOfPoll;
        }
        LOGGER.info("finished creating fix1440 data...");
        
        

        ///////////// get data from fix720Time \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fix720 data...");
        culmulatedVolumeSB.append("fix720;");
        culmulatedVolumePerTechnique = 0;
        feedIDLastStep = -1;
        pollingInterval = 12;
        requiredNumberOfPolls = totalExperimentHours/pollingInterval;
        
        polls = ed.getSumTransferVolumeByHourFromFix720MaxMinTime();
       
        for (EvaluationFeedPoll poll : polls) {
            Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            int feedIDCurrent = poll.getFeedID();
            
            // in der DB nicht vorhandene Polls generieren 
            if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                while (numberOfPollsToProcess > 0){
                    numberOfPollsToProcess--;
                    int hourToProcess = hourLastStep + pollingInterval;
                    long culmulatedVolumePerHour = 0;
                    transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                    
                    if( totalResultMapMin.containsKey(hourToProcess)){
                        transferredDataArray = totalResultMapMin.get(hourToProcess);
                        if (transferredDataArray[1] != null) {
                            culmulatedVolumePerHour = transferredDataArray[1];
                        }                        
                    }
                    culmulatedVolumePerHour += sizeOfPollLast;
                    transferredDataArray[1] = culmulatedVolumePerHour;                                        
                    totalResultMapMin.put(hourToProcess, transferredDataArray);
                    hourLastStep = hourToProcess;
                }                
                // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
                numberOfPollsToProcess = requiredNumberOfPolls;
            }
                   
            // aktuellen Poll behandeln
            int hourToProcess = poll.getHourOfExperiment();
            long culmulatedVolumePerHour = 0;
            transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            
            if( totalResultMapMin.containsKey(hourToProcess)){
                transferredDataArray = totalResultMapMin.get(hourToProcess);
                if (transferredDataArray[1] != null) {
                    culmulatedVolumePerHour = transferredDataArray[1];
                }                        
            }
            float sizeOfPoll = poll.getSizeOfPoll();
            culmulatedVolumePerHour += sizeOfPoll;
            transferredDataArray[1] = culmulatedVolumePerHour;                                        
            totalResultMapMin.put(hourToProcess, transferredDataArray);
            
            numberOfPollsToProcess--;
            hourLastStep = hourToProcess;
            feedIDLastStep = feedIDCurrent;
            sizeOfPollLast = sizeOfPoll;
        }
        LOGGER.info("finished creating fix720 data...");
        

        ///////////// get data from fix60Time \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fix60 data...");
        culmulatedVolumeSB.append("fix60;");
        culmulatedVolumePerTechnique = 0;
        feedIDLastStep = -1;
        pollingInterval = 1;
        requiredNumberOfPolls = totalExperimentHours/pollingInterval;
        
        int feedIDStart = 1;
        int feedIDEnd = 10000;
        final int FEED_ID_STEP = 10000;
        final int FEED_ID_MAX = 210000;
        
        while(feedIDEnd < FEED_ID_MAX){
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
            polls = ed.getSumTransferVolumeByHourFromFix60MaxMinTime(feedIDStart, feedIDEnd);
            numberOfPollsToProcess = requiredNumberOfPolls;
            
            for (EvaluationFeedPoll poll : polls) {
                Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                int feedIDCurrent = poll.getFeedID();
                
                // in der DB nicht vorhandene Polls generieren 
                if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                    while (numberOfPollsToProcess > 0){
                        int hourToProcess = hourLastStep + pollingInterval;
                        long culmulatedVolumePerHour = 0;
                        transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                        
                        if( totalResultMapMin.containsKey(hourToProcess)){
                            transferredDataArray = totalResultMapMin.get(hourToProcess);
                            if (transferredDataArray[2] != null) {
                                culmulatedVolumePerHour = transferredDataArray[2];
                            }                        
                        }
                        culmulatedVolumePerHour += sizeOfPollLast;
                        transferredDataArray[2] = culmulatedVolumePerHour;                                        
                        totalResultMapMin.put(hourToProcess, transferredDataArray);
                        hourLastStep = hourToProcess;
                        numberOfPollsToProcess--;
                    }                
                    // Wert für den eigentlich zu bearbeitenden Poll zurücksetzen, da nun neue FeedID behandelt wird
                    numberOfPollsToProcess = requiredNumberOfPolls;
                }
                       
                // aktuellen Poll behandeln
                int hourToProcess = poll.getHourOfExperiment();
                long culmulatedVolumePerHour = 0;
                transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                
                if( totalResultMapMin.containsKey(hourToProcess)){
                    transferredDataArray = totalResultMapMin.get(hourToProcess);
                    if (transferredDataArray[2] != null) {
                        culmulatedVolumePerHour = transferredDataArray[2];
                    }                        
                }
                float sizeOfPoll = poll.getSizeOfPoll();
                culmulatedVolumePerHour += sizeOfPoll;
                transferredDataArray[2] = culmulatedVolumePerHour;                                        
                totalResultMapMin.put(hourToProcess, transferredDataArray);
                
                numberOfPollsToProcess--;
                hourLastStep = hourToProcess;
                feedIDLastStep = feedIDCurrent;
                sizeOfPollLast = sizeOfPoll;
            }
            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating fix60 data...");
        
        
        
        ///////////// get data from fixLearnedTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create fixLearned data...");
        culmulatedVolumeSB.append("fixLearned;");
        culmulatedVolumePerTechnique = 0;
        feedIDLastStep = -1;
        numberOfPollsToProcess = -1;
        feedIDStart = 1;
        feedIDEnd = 10000;
        
        while(feedIDEnd < FEED_ID_MAX){
            LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);            
            int minuteLastStep = 0;                        
            
            polls = ed.getSumTransferVolumeByHourFromFixLearnedMinTime(feedIDStart, feedIDEnd);
            
            for (EvaluationFeedPoll poll : polls) {
                Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                int feedIDCurrent = poll.getFeedID();
                
                // in der DB nicht vorhandene Polls generieren 
                if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                    while (minuteLastStep < totalExperimentHours*60){
                        
                        int minuteToProcess = (int)(minuteLastStep + poll.getCheckInterval());
                        int hourToProcess = minuteToProcess/60;
                        long culmulatedVolumePerHour = 0;
                        transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                        
                        if( totalResultMapMin.containsKey(hourToProcess)){
                            transferredDataArray = totalResultMapMin.get(hourToProcess);
                            if (transferredDataArray[3] != null) {
                                culmulatedVolumePerHour = transferredDataArray[3];
                            }                        
                        }
                        culmulatedVolumePerHour += sizeOfPollLast;
                        transferredDataArray[3] = culmulatedVolumePerHour;                                        
                        totalResultMapMin.put(hourToProcess, transferredDataArray);                        
                        minuteLastStep = minuteToProcess;
                        numberOfPollsToProcess--;
                    }
                }                      
                
                // aktuellen Poll behandeln                
                int hourToProcess = poll.getHourOfExperiment();
                long culmulatedVolumePerHour = 0;
                transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
                
                if( totalResultMapMin.containsKey(hourToProcess)){
                    transferredDataArray = totalResultMapMin.get(hourToProcess);
                    if (transferredDataArray[3] != null) {
                        culmulatedVolumePerHour = transferredDataArray[3];
                    }                        
                }
                float sizeOfPoll = poll.getSizeOfPoll();
                culmulatedVolumePerHour += sizeOfPoll;
                transferredDataArray[3] = culmulatedVolumePerHour;                                        
                totalResultMapMin.put(hourToProcess, transferredDataArray);
                
                if(poll.getNumberOfPoll() > 2) {
                    minuteLastStep += (int)poll.getCheckInterval();
                }
                feedIDLastStep = feedIDCurrent;
                sizeOfPollLast = sizeOfPoll;
            }
            feedIDStart += FEED_ID_STEP;
            feedIDEnd += FEED_ID_STEP;
        }
        LOGGER.info("finished creating fixLearned data...");
        
                
        
        ///////////// get data from adaptiveMaxTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create adaptive data...");
        culmulatedVolumeSB.append("adaptive;");
        culmulatedVolumePerTechnique = 0;
        polls = ed.getSumTransferVolumeByHourFromAdaptiveMinTime();
        for (EvaluationFeedPoll poll : polls) {
            Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            int hourToProcess = poll.getHourOfExperiment();            
            if( totalResultMapMin.containsKey(hourToProcess)){
                transferredDataArray = totalResultMapMin.get(hourToProcess);
            }
            
            culmulatedVolumePerTechnique += poll.getCulmulatedSizeofPolls();
            transferredDataArray[4] = culmulatedVolumePerTechnique;
            totalResultMapMin.put(hourToProcess, transferredDataArray);
        }
        LOGGER.info("finished creating adaptive data...");

        
        ///////////// get data from probabilisticMaxTime \\\\\\\\\\\\\\\\\
        LOGGER.info("starting to create probabilistic data...");
        culmulatedVolumeSB.append("probabilistic;\n");
        culmulatedVolumePerTechnique = 0;
        polls = ed.getSumTransferVolumeByHourFromProbabilisticMinTime();
        for (EvaluationFeedPoll poll : polls) {
            Long[] transferredDataArray = new Long[]{0l,0l,0l,0l,0l,0l};
            int hourToProcess = poll.getHourOfExperiment();            
            if( totalResultMapMin.containsKey(hourToProcess)){
                transferredDataArray = totalResultMapMin.get(hourToProcess);
            }
            
            culmulatedVolumePerTechnique += poll.getCulmulatedSizeofPolls();
            transferredDataArray[5] = culmulatedVolumePerTechnique;
            totalResultMapMin.put(hourToProcess, transferredDataArray);
        }
        LOGGER.info("finished creating probabilistic data...");

                
        int i = 1;
        final long BYTE_TO_MB = 1048576;
        Long[] volumesCumulated = new Long[]{0l,0l,0l,0l,0l,0l};
        for(Long[] volumes : totalResultMapMin.values()){
            volumesCumulated[0] += volumes[0];
            volumesCumulated[1] += volumes[1];
            volumesCumulated[2] += volumes[2];
            volumesCumulated[3] += volumes[3];
            culmulatedVolumeSB.append(i).append(";")
                .append(volumesCumulated[0]/BYTE_TO_MB).append(";")
                .append(volumesCumulated[1]/BYTE_TO_MB).append(";")
                .append(volumesCumulated[2]/BYTE_TO_MB).append(";")
                .append(volumesCumulated[3]/BYTE_TO_MB).append(";")
                .append(volumes[4]/BYTE_TO_MB).append(";")
                .append(volumes[5]/BYTE_TO_MB).append(";\n");            
            i++;   
        }
        
        FileHelper.writeToFile(sumVolumeMinFilePath, culmulatedVolumeSB);
        LOGGER.info("sumVolumeMinFile *hopefully* :) written to: " + sumVolumeMinFilePath);
    } 
    
    
    
    
    
    /**
     * simply testing the connection and EvaluationFeedPoll class.
     * */
    private void firstTest(){
        EvaluationDatabase ed = EvaluationDatabase.getInstance();
        List<EvaluationFeedPoll> polls = ed.getFeedPolls();
        for (EvaluationFeedPoll poll : polls) {
            System.out.println(poll);
        }
    }
        
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    
	    ChartCreator cc = new ChartCreator();

//	    cc.createFeedSizeHistogrammFile();
//	    cc.createFeedAgeFile();
//	    cc.createTimeliness2File();
//	    cc.createPercentageNewFile();
	    cc.culmulatedVolumeMaxTimeFile();
//      cc.culmulatedVolumeMinTimeFile();
	    	    

	}

}
