package hec.ensemble;

import java.sql.*;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 *  Read/Write Ensembles to a JDBC database
 */
public class JdbcEnsembleDatabase implements AutoCloseable {

    static String DateTimeFormat = "yyyy-MM-dd HH:mm:ss";
    private static String TableName = "timeseries_ensemble";

    String FileName;
    Connection _connection;
    public JdbcEnsembleDatabase(String fileName) throws Exception
    {
      FileName= fileName;
      Properties prop = new Properties();
      //prop.setProperty("shared_cache", "false");
        //  Synchronous=Off;Pooling=True;Journal Mode=Off";  // dangerous but faster.
        //
      _connection =  DriverManager.getConnection("jdbc:sqlite:"+FileName,prop);
      _connection.setAutoCommit(false);
        CreateTable();
    }

    public void close()
    {
        try {
            _connection.commit();
            _connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    PreparedStatement insertCMD;

    public void Write(Watershed watershed) throws Exception
    {
        try {
            String sql = "INSERT INTO "+TableName+" ([id], [issue_date], [watershed], [location_name], "+
                    " [timeseries_start_date], [member_length], [member_count], [compressed], [byte_value_array]) VALUES "+
                    "(?, ?, ?, ?, ?, ?, ?, ?, ?)";

            insertCMD = _connection.prepareStatement(sql);

            boolean compress = true;
            int index = GetNextID();
            for(Location loc : watershed.Locations)
            {
                for(Forecast f : loc.Forecasts)
                {
                    index++;
                    float[][] data = f.Ensemble;
                    byte[] bytes = EnsembleCompression.Pack(data,compress);
                    int len = bytes.length;
                     InsertEnsemble(index, f.IssueDate, watershed.Name, loc.Name, f.startDateTime,
                            data[0].length, data.length, compress, bytes);
                }
            }
            _connection.commit();
        }catch(Exception e)
        {
            Logger.logError("writing ensembles "+e.getMessage());
        }
        finally {
           // if( _connection!= null)
             //   _connection.close();
        }

    }

    public Watershed Read(String watershedName, ZonedDateTime startTime, ZonedDateTime endTime)
    {
        Watershed rval = new Watershed(watershedName);

        String sql = "select * from " + TableName +
                " WHERE issue_date >= '" + FormatDate(startTime) + "' "
                + " AND issue_date <= '" + FormatDate(endTime) + "' "
                + " AND watershed = '" + watershedName + "' ";
        sql += " order by watershed,issue_date,location_name";


        try {
            Statement stmt  = _connection.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);
            // loop through the result set
            while (rs.next()) {
                int id = rs.getInt(1);
                String d = rs.getString(2);
                ZonedDateTime issue_date = DateUtility.ParseDateTime(d);

                //watershedName = rs.getString(3,);
                String locName = rs.getString(4);
                d = rs.getString(5);
                ZonedDateTime start_date =  DateUtility.ParseDateTime(d);
                int member_length = rs.getInt(6 );
                int member_count = rs.getInt(7);
                boolean compressed = rs.getBoolean(8);
                byte[] byte_value_array = rs.getBytes(9);
                float[][] ensemble = EnsembleCompression.UnPack(byte_value_array,member_count,member_length,compressed);
                int secondsPerHour = 3600;// TO DO .. FIX ME hardcoded (need to add increment to schema)
                //ZonedDateTime[] timeStamps = GetTimeStamps(start_date,member_length,secondsPerHour);
                rval.AddForecast(locName,issue_date,ensemble,start_date, Duration.ofSeconds(secondsPerHour));
            }
        } catch (Exception e) {
            Logger.logError(e.getMessage());
        }
//        ZonedDateTime prevIssueDate = Convert.ToDateTime(table.Rows[0]["issue_date"]);
//        DateTime currentDate = Convert.ToDateTime(table.Rows[0]["issue_date"]);
//        foreach (DataRow row in table.Rows)
//        {
//            currentDate = Convert.ToDateTime(row["issue_date"]);
//
//            var times = GetTimes(row);
//            GetValues(row,ref values);
//
//            rval.AddForecast(row["location_name"].ToString(),
//                    currentDate,
//                    values,
//                    times);
//
//        }
        return rval;
    }

    private static ZonedDateTime[] GetTimeStamps(ZonedDateTime t1, int count, int secondsIncrement)
    {
        ZonedDateTime[] rval = new ZonedDateTime[count];
        for (int i = 0; i < count; i++)
        {
            rval[i] = t1;
            t1 = t1.plusSeconds(secondsIncrement);
        }
        return rval;
    }
    static DateTimeFormatter _formatter = DateTimeFormatter.ofPattern(DateTimeFormat);
    private static String FormatDate(ZonedDateTime t)
    {
     return t.format(_formatter);
    }

    private void InsertEnsemble(int id, ZonedDateTime issue_date, String watershed, String location_name,
                               ZonedDateTime timeseries_start_date,int member_length, int member_count,boolean compressed,
                               byte[] byte_value_array) throws Exception
    {


        insertCMD.setInt(1, id);
        insertCMD.setString(2,FormatDate(issue_date));
        insertCMD.setString(3,watershed);
        insertCMD.setString(4,location_name);
        insertCMD.setString(5, FormatDate(timeseries_start_date));
        insertCMD.setInt(6, member_length);
        insertCMD.setInt(7, member_count);
        insertCMD.setBoolean(8, compressed  );
        insertCMD.setBytes(9, byte_value_array);
        insertCMD.execute();
    }


    private int GetNextID()
    {
        String sql = "SELECT max(id) max FROM "+TableName;
        int rval = 0;
        try {
            Statement stmt  = _connection.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);
            // loop through the result set
            while (rs.next()) {
                Object o = rs.getObject("max");
                if( o == null)
                    return 0;
                rval = (int)o;
            }
        } catch (Exception e) {
            Logger.logError(e.getMessage());
        }
        return rval;
    }

    private void  CreateTable()throws Exception
    {
        String sql = "CREATE TABLE IF NOT EXISTS " + TableName
                + " ( id integer not null primary key,"
                + "    issue_date datetime, "
                + "   watershed NVARCHAR(100) ,"
                + "   location_name NVARCHAR(100) ,"
                + "   timeseries_start_date datetime ,"
                + "   member_length integer    ,"
                + "   member_count integer    ,"
                + "   compressed integer    ,"
                + "  byte_value_array BLOB NULL )";
        PreparedStatement cmd = _connection.prepareStatement(sql);
        cmd.execute();
    }

    /**
     * insertPiscesCatalog adds entries to the pisces series catalog
     */
    private void insertPiscesCatalog(){

        //

    }


}