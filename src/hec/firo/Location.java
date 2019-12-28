package hec.firo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Location
  {
    public Location(String name, Watershed watershed)
    {
      this.Name = name;
      Forecasts = new ArrayList<>();
      this.Watershed = watershed;
    }
    public String Name;

    /// <summary>
    /// Parent Watershed
    /// </summary>
    public Watershed Watershed;
    
    /// <summary>
    /// List of forecasts 
    /// </summary>
    public ArrayList<Forecast> Forecasts;

     Forecast AddForecast(LocalDateTime issueDate, float[][] ensemble, LocalDateTime[] timeStamps)
    {
      Forecast f = new Forecast(this, issueDate,ensemble,timeStamps);
      Forecasts.add(f);
      return f;
    }
  }
