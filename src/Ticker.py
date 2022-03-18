class Ticker(object):
    TICKER_PREFIX = "ticker:"

    def __init__(self):
        self.Market = ""
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


    def __init__(self, **kwargs):
        self.Market = ""
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
        self.MostRecent = "false"
        for key in kwargs:
            if key == "<TICKER>":
                objkey = "Ticker"
                # also set the short ticker and the Market
                split_ticker = kwargs[key].split('.')
                short_ticker = split_ticker[0]
                market = split_ticker[1]
                setattr(self,"TickerShort", short_ticker)
                setattr(self, "Market", market)
            elif key == "<PER>":
                objkey = "Per"
            elif key == "<DATE>":
                objkey = "Date"
            elif key == "<OPEN>":
                objkey = "Open"
            elif key == "<TIME>":
                objkey = "Time"
            elif key == "<HIGH>":
                objkey = "High"
            elif key == "<LOW>":
                objkey = "Low"
            elif key == "<CLOSE>":
                objkey = "Close"
            elif key == "<VOL>":
                objkey = "Volume"
            elif key == "<OPENINT>":
                objkey = "OpenInt"
            else:
                objkey = key
            setattr(self, objkey, kwargs[key])

    def __str__(self):
        return str(self.__dict__)

    def get_key(self):
        return str(self.TICKER_PREFIX + self.Ticker + ':' + str(self.Date))
