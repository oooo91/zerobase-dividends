package com.devideds.service;

import com.devideds.model.Company;
import com.devideds.model.Dividend;
import com.devideds.model.ScrapedResult;
import com.devideds.model.constants.CacheKey;
import com.devideds.persist.CompanyRespository;
import com.devideds.persist.DividendRepository;
import com.devideds.persist.entity.CompanyEntity;
import com.devideds.persist.entity.DividendEntity;
import com.devideds.exception.impl.NoCompanyException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRespository companyRespository;
    private final DividendRepository dividendRepository;

    //요청이 자주 들어오는가?
    //자주 변경되는 데이터인가?
    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("search company -> " + companyName);

        //1.회사명을 기준으로 회사 정보를 조회
        CompanyEntity company = this.companyRespository.findByName(companyName)
                .orElseThrow(() -> new NoCompanyException());

        //2.조회된 회사 ID로 배당급 정보 조회
        List<DividendEntity> dividendEntities = this.dividendRepository.findAllByCompanyId(company.getId());

        //3.결과 조합 후 반환
        /* 또는
        List<Dividend> dividends = new ArrayList<>();
        for (var entity : dividendEntities) {
            dividends.add(Dividend.builder()
                            .date(entity.getDate())
                            .dividend(entity.getDividend())
                            .build());
         List<Dividend> dividends =  dividendEntities.stream()
                .map(e -> Dividend.builder()
                        .date(e.getDate())
                        .dividend(e.getDividend())
                        .build())
                .collect(Collectors.toList());
        }
        */
        List<Dividend> dividends =  dividendEntities.stream()
                .map(e -> new Dividend(e.getDate(), e.getDividend()))
                .collect(Collectors.toList());

        return new ScrapedResult(new Company(company.getTicker(), company.getName()),
                dividends);
    }
}
