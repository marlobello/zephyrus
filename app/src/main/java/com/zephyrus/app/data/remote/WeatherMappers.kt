package com.zephyrus.app.data.remote

import com.zephyrus.app.data.model.AirQualityResponse
import com.zephyrus.app.data.model.CurrentAirQuality
import com.zephyrus.app.data.model.CurrentData
import com.zephyrus.app.data.model.DailyData
import com.zephyrus.app.data.model.HourlyData
import com.zephyrus.app.data.model.WeatherResponse
import com.zephyrus.app.domain.model.CurrentWeather
import com.zephyrus.app.domain.model.DailyForecast
import com.zephyrus.app.domain.model.HourlyForecast
import com.zephyrus.app.domain.model.PollenData
import com.zephyrus.app.domain.model.WeatherCondition

fun WeatherResponse.toCurrentWeather(airQuality: AirQualityResponse?): CurrentWeather? {
    val c = current ?: return null
    return c.toCurrentWeather(airQuality?.current)
}

fun CurrentData.toCurrentWeather(pollen: CurrentAirQuality?): CurrentWeather {
    return CurrentWeather(
        temperature = temperature,
        feelsLike = apparentTemperature,
        humidity = relativeHumidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        pressure = surfacePressure,
        uvIndex = uvIndex,
        condition = WeatherCondition.fromWmoCode(weatherCode),
        isDay = isDay == 1,
        pollen = pollen?.toPollenData(),
    )
}

fun CurrentAirQuality.toPollenData(): PollenData {
    return PollenData(
        grassPollen = grassPollen,
        treePollen = birchPollen,
        weedPollen = ragweedPollen,
    )
}

fun HourlyData.toHourlyForecasts(): List<HourlyForecast> {
    return time.indices.map { i ->
        HourlyForecast(
            time = time[i],
            temperature = temperature[i],
            humidity = relativeHumidity[i],
            condition = WeatherCondition.fromWmoCode(weatherCode[i]),
            isDay = isDay[i] == 1,
        )
    }
}

fun DailyData.toDailyForecasts(pollenByDay: Map<String, PollenData>? = null): List<DailyForecast> {
    return time.indices.map { i ->
        DailyForecast(
            date = time[i],
            temperatureMax = temperatureMax[i],
            temperatureMin = temperatureMin[i],
            condition = WeatherCondition.fromWmoCode(weatherCode[i]),
            windSpeedMax = windSpeedMax[i],
            windDirection = windDirection[i],
            humidity = humidityMean.getOrElse(i) { 0 },
            precipitationProbability = precipitationProbabilityMax[i],
            uvIndexMax = uvIndexMax[i],
            pollen = pollenByDay?.get(time[i]),
        )
    }
}
