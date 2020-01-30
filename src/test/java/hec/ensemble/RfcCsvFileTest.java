package hec.ensemble;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RfcCsvFileTest {


    @Test
    public void ReadCsv() {

        String fn = TestingPaths.instance.getTestCsvFileName();
        RfcCsvFile csv = new RfcCsvFile(fn);
        float[][] data = csv.getEnsemble("SCRN2");

        AssertSCRN2(data);
        float[][] susc1 = csv.getEnsemble("SUSC1");

        assertEquals(1000.0f, susc1[0][0], 0.0001);
        assertEquals(2000.0f, susc1[0][1], 0.0001);
        assertEquals(3000.0f, susc1[0][2], 0.0001);


    }

    private void AssertSCRN2(float[][] data) {
        assertEquals(-1000.0f, data[0][0], 0.0001);
        assertEquals(-2100f, data[0][1], 0.0001);
        assertEquals(-3100f, data[0][2], 0.0001);
        assertEquals(-59000.0f, data[58][0], 0.0001);
        assertEquals(-59100f, data[58][1], 0.0001);
        assertEquals(-59200f, data[58][2], 0.0001);
    }


    /**
     * Reads CSV, saves one ensemble to a database, then reads ensemble back in.
     */
    @Test
    public void importCsvToDatabase() {
        try {

            String fn = "test.db";
            File f = new File(fn);
            f.delete();

            EnsembleTimeSeriesDatabase db  = DatabaseGenerator.createTestDatabase(fn,1);
            // --- READ
            TimeSeriesIdentifier tsid = new TimeSeriesIdentifier("Kanektok.SCRN2","flow");
            EnsembleTimeSeries ets =  db.getEnsembleTimeSeries(tsid);
            List<ZonedDateTime> issueDates = ets.getIssueDates();
            Ensemble e = db.getEnsemble(tsid, issueDates.get(0));
            float[][] data2 = e.getValues();
            AssertSCRN2(data2);

        } catch (Exception e) {
            Logger.logError(e);
            fail();
        }
    }

}