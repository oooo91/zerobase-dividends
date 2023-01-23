package com.devideds.service;

import com.devideds.model.Company;
import com.devideds.model.ScrapedResult;
import com.devideds.persist.CompanyRespository;
import com.devideds.persist.DividendRepository;
import com.devideds.persist.entity.CompanyEntity;
import com.devideds.persist.entity.DividendEntity;
import com.devideds.scraper.Scraper;
import com.devideds.exception.impl.NoCompanyException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.apache.commons.collections4.Trie;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyService {

    private final Trie trie; //공유 변수
    private final Scraper yahooFinanceScraper;

    private final CompanyRespository companyRespository;
    private final DividendRepository dividendRepository;

    //ticker가 존재하지 않으면 ticker 저장
    public Company save(String ticker) {
        boolean exists = this.companyRespository.existsByTicker(ticker);
        if (exists) {
            throw new RuntimeException("already exists ticker -> " + ticker);
        }
        return this.storeCompanyAndDividend(ticker);
    }

    //회사 정보
    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return this.companyRespository.findAll(pageable);
    }

    private Company storeCompanyAndDividend(String ticker) {
        //ticker를 기준으로 회사를 스크래핑
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);

        if (ObjectUtils.isEmpty(company)) {
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }

        //해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

        //스크래핑 결과
        CompanyEntity companyEntity = this.companyRespository.save(new CompanyEntity(company));
        List<DividendEntity> list = scrapedResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e)) // e는 Dividend 요소 하나하나
                .collect(Collectors.toList());

        this.dividendRepository.saveAll(list);
        return company;
    }

    public List<String> getCompanyNamesByKeyword(String keyword) {
        Pageable limit = PageRequest.of(0, 10);
        Page<CompanyEntity> companyEntities = this.companyRespository.findByNameStartingWithIgnoreCase(keyword, limit);
        return companyEntities.stream()
                .map(e -> e.getName()).collect(Collectors.toList());
    }

    //데이터 저장
    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null); //아파치의 trie는 key-value를 저장할 수 있는데, value는 자동완성에 필요 x
    }

    //데이터 검색 앤 결과 - 회사 목록 리스트
    public List<String> autocomplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream().limit(10).collect(Collectors.toList()); //데이터가 많으면 page 또는 limit를 통해 개수를 조절할 수 있다.
    }

    //데이터 삭제
    public void deleteAutoCompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        var company = this.companyRespository.findByTicker(ticker)
                .orElseThrow(() -> new NoCompanyException());

        this.dividendRepository.deleteAllByCompanyId(company.getId());
        this.companyRespository.delete(company);

        //trie 삭제
        this.deleteAutoCompleteKeyword(company.getName());
        return company.getName();
    }
}
