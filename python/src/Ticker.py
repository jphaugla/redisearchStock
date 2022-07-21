class Ticker(object):
    TICKER_PREFIX = "ticker:"

    def __init__(self):
        self.geography = ""
        self.ticker = ""
        self.tickershort = ""
        self.per = ""
        self.Date = 0
        self.time = 0
        self.open = 0.0
        self.high = 0.0
        self.low = 0.0
        self.close = 0.0
        self.volume = 0
        self.openint = ""
        self.mostrecent = "false"
        self.exchange = ""


    def __init__(self, **kwargs):
        self.geography = ""
        self.ticker = ""
        self.tickershort = ""
        self.per = ""
        self.date = 0
        self.time = 0
        self.open = 0.0
        self.high = 0.0
        self.low = 0.0
        self.close = 0.0
        self.volume = 0
        self.openint = ""
        self.score = 1.0
        self.mostrecent = "false"
        self.exchange = ""
        for key in kwargs:
            # print("key is " + str(key) + " value is " + kwargs[key])
            value_type = "string"
            if key == "<TICKER>":
                objkey = "ticker"
                # also set the short ticker and the geography
                split_ticker = kwargs[key].split('.')
                short_ticker = split_ticker[0]
                geography = split_ticker[1]
                setattr(self,"tickershort", short_ticker.strip('\"'))
                setattr(self, "geography", geography.strip('\"'))
            elif key == "<PER>":
                objkey = "per"
            elif key == "<DATE>":
                objkey = "date"
                value_type = "int"
            elif key == "<OPEN>":
                objkey = "open"
                value_type = "float"
            elif key == "<TIME>":
                objkey = "time"
                value_type = "int"
            elif key == "<HIGH>":
                objkey = "high"
                value_type = "float"
            elif key == "<LOW>":
                objkey = "low"
                value_type = "float"
            elif key == "<CLOSE>":
                objkey = "close"
                value_type = "float"
            elif key == "<VOL>":
                objkey = "volume"
                value_type = "float"
            elif key == "<OPENINT>":
                objkey = "openint"
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
        return str(self.TICKER_PREFIX + self.ticker + ':' + str(self.date))
