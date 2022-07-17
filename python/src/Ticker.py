class Ticker(object):
    TICKER_PREFIX = "ticker:"

    def __init__(self):
        self.Geography = ""
        self.Ticker = ""
        self.Per = ""
        self.Date = 0
        self.Time = 0
        self.Open = 0.0
        self.High = 0.0
        self.Low = 0.0
        self.Close = 0.0
        self.Volume = 0
        self.OpenInt = ""
        self.MostRecent = "false"
        self.Exchange = ""


    def __init__(self, **kwargs):
        self.Geography = ""
        self.Ticker = ""
        self.TickerShort = ""
        self.Per = ""
        self.Date = 0
        self.Time = 0
        self.Open = 0.0
        self.High = 0.0
        self.Low = 0.0
        self.Close = 0.0
        self.Volume = 0
        self.OpenInt = ""
        self.Score = 1.0
        self.MostRecent = "false"
        self.Exchange = ""
        for key in kwargs:
            # print("key is " + str(key) + " value is " + kwargs[key])
            value_type = "string"
            if key == "<TICKER>":
                objkey = "Ticker"
                # also set the short ticker and the Geography
                split_ticker = kwargs[key].split('.')
                short_ticker = split_ticker[0]
                geography = split_ticker[1]
                setattr(self,"TickerShort", short_ticker.strip('\"'))
                setattr(self, "Geography", geography.strip('\"'))
            elif key == "<PER>":
                objkey = "Per"
            elif key == "<DATE>":
                objkey = "Date"
                value_type = "int"
            elif key == "<OPEN>":
                objkey = "Open"
                value_type = "float"
            elif key == "<TIME>":
                objkey = "Time"
                value_type = "int"
            elif key == "<HIGH>":
                objkey = "High"
                value_type = "float"
            elif key == "<LOW>":
                objkey = "Low"
                value_type = "float"
            elif key == "<CLOSE>":
                objkey = "Close"
                value_type = "float"
            elif key == "<VOL>":
                objkey = "Volume"
                value_type = "float"
            elif key == "<OPENINT>":
                objkey = "OpenInt"
                value_type = "int"
            else:
                objkey = key
            if value_type == "int":
                setattr(self, objkey, int(kwargs[key]))
            elif value_type == "float":
                setattr(self, objkey, float(kwargs[key]))
            else:
                setattr(self, objkey, kwargs[key])


    def __str__(self):
        return str(self.__dict__)

    def get_key(self):
        return str(self.TICKER_PREFIX + self.Ticker + ':' + str(self.Date))
