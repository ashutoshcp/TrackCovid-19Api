package com.trackcovid19.service;

import com.trackcovid19.Repository.TrackCovid19LastUpdateRepo;
import com.trackcovid19.Repository.TrackCovid19Repo;
import com.trackcovid19.model.CovidChartData;
import com.trackcovid19.model.LastUpdated;
import com.trackcovid19.model.StateWiseData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class TrackCovid19Service {
    @Autowired
    private TrackCovid19Repo trackCovid19Repo;

    @Autowired
    private TrackCovid19LastUpdateRepo trackCovid19LastUpdateRepo;



    public StateWiseData createPerState(StateWiseData stateWiseData) {
        List<StateWiseData> state = trackCovid19Repo.findByStateName(stateWiseData.getStateName());
        if (null != state && state.size() > 0) {
            state.get(0).setConfirmedCases(stateWiseData.getConfirmedCases());
            state.get(0).setRecoveredCases(stateWiseData.getRecoveredCases());
            state.get(0).setDeceased(stateWiseData.getDeceased());
            return trackCovid19Repo.save(state.get(0));
        }
        return trackCovid19Repo.save(stateWiseData);
    }

    public List<StateWiseData> getAllState() {
        return trackCovid19Repo.findAll();
    }

    public LastUpdated createLastUpdate(LastUpdated lastUpdated) {
        Optional<LastUpdated> ls = trackCovid19LastUpdateRepo.findById("first");
        LastUpdated lu;
        if (!ls.isPresent()) {
            lu = trackCovid19LastUpdateRepo.save(lastUpdated);
        } else {
            lu = ls.get();
            lu.setLastUpdated(new Date());
            lu = trackCovid19LastUpdateRepo.save(lu);

        }

        return lu;
    }

    public LastUpdated fetchLastUpdated() {
        Optional<LastUpdated> ls = trackCovid19LastUpdateRepo.findById("first");
        LastUpdated lu = ls.get();
        this.timeDiffCalc(lu.getLastUpdated(), new Date(), lu);
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM hh:mm:ss a");
        formatter.setTimeZone(TimeZone.getTimeZone("IST"));
        String formattedDateTime = formatter.format(lu.getLastUpdated());
        String[] s = formattedDateTime.split(" ");
        lu.setDate(s[0]);
        lu.setTime(s[1] + " " + s[2]);
        return lu;
    }

    public void timeDiffCalc(Date d1, Date d2, LastUpdated lu) {


        try {

            //in milliseconds
            long diff = d2.getTime() - d1.getTime();
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            lu.setTimeDiffHr(diffHours);
            lu.setTimeDiffMn(diffMinutes);
            if (diffHours == 0) {

                lu.setTimeDiffText("About " + diffMinutes + " Minutes Ago");
            } else if (diffMinutes == 0) {

                lu.setTimeDiffText("About " + diffHours + " Hours Ago");
            } else {

                lu.setTimeDiffText("About " + diffHours + " Hours " + diffMinutes + " Minutes Ago");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void readFromXLSAndUpdate() {
        try {
            String state = new String();
            String activeCases = null;
            String recovered = null;
            String deceased = null;

            File file = new File(getClass().getClassLoader().getResource("covid.xslx").getFile());   //creating a new file instance
            FileInputStream fis = new FileInputStream(file);   //obtaining bytes from the file
//creating Workbook instance that refers to .xlsx file
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0);     //creating a Sheet object to retrieve object
            Iterator<Row> itr = sheet.iterator();    //iterating over excel file
            while (itr.hasNext()) {
                Row row = itr.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                //iterating over each column
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    if (cell.getColumnIndex() == 1) {
                        state = cell.getStringCellValue();
                    }
                    if (cell.getColumnIndex() == 2) {

                        activeCases = String.valueOf(Math.round(cell.getNumericCellValue()));
                    }
                    if (cell.getColumnIndex() == 3) {
                        recovered = String.valueOf(Math.round(cell.getNumericCellValue()));
                    }
                    if (cell.getColumnIndex() == 4) {
                        deceased = String.valueOf(Math.round(cell.getNumericCellValue()));
                    }

                }
                StateWiseData stateWiseData = new StateWiseData(state, activeCases, recovered, deceased);
                this.createPerState(stateWiseData);
            }

            LastUpdated lastUpdated = new LastUpdated();
            lastUpdated.setLastUpdated(new Date());
            this.createLastUpdate(lastUpdated);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


public CovidChartData readChartDataFromExcel() {

    try {
        String month = new String();
        Long totalConfirmed = null;
        CovidChartData covidChartData = new CovidChartData();
        covidChartData.setxAxis(new ArrayList<>());
        covidChartData.setyAxis(new ArrayList<>());
          //creating a new file instance
        //creating a new file instance
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("covid.xslx");
           //obtaining bytes from the file
//creating Workbook instance that refers to .xlsx file
        XSSFWorkbook wb = new XSSFWorkbook(is);
        XSSFSheet sheet = wb.getSheetAt(1);     //creating a Sheet object to retrieve object
        Iterator<Row> itr = sheet.iterator();    //iterating over excel file
        while (itr.hasNext()) {
            Row row = itr.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            //iterating over each column
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                if (cell.getColumnIndex() == 0) {
                    month = cell.getStringCellValue();
                    covidChartData.getxAxis().add(month);
                }
                if (cell.getColumnIndex() == 1) {

                    totalConfirmed = Math.round(cell.getNumericCellValue());
                    covidChartData.getyAxis().add(totalConfirmed);
                }


            }

        }

        return covidChartData;

    } catch (Exception e) {
        e.printStackTrace();
    }
return null;
}

}