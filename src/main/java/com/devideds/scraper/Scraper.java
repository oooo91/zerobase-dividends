package com.devideds.scraper;

import com.devideds.model.Company;
import com.devideds.model.ScrapedResult;

public interface Scraper {
    Company scrapCompanyByTicker(String ticker);
    ScrapedResult scrap(Company company);
}
