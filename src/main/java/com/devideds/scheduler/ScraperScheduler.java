package com.devideds.scheduler;

import com.devideds.model.Company;
import com.devideds.model.ScrapedResult;
import com.devideds.model.constants.CacheKey;
import com.devideds.persist.CompanyRespository;
import com.devideds.persist.DividendRepository;
import com.devideds.persist.entity.CompanyEntity;
import com.devideds.persist.entity.DividendEntity;
import com.devideds.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
@EnableCaching
public class ScraperScheduler {

    private final CompanyRespository companyRespository;
    private final DividendRepository dividendRepository;

    private final Scraper yahooFinanceScraper;

    //일정 주기마다 수행
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true) //db를 업데이트할 경우 캐시도 같이 업데이트를 해주거나 비워야함, 메모리 위해 오래된 데이터는 비워야함
    @Scheduled(cron = "${scheduler.scrap.yahoo}") //config 설정으로 따로 관리 -> 다시 배포하지 않아도 바뀐 설정값 적용
    public void yahooFinanceScheduling() {

        //저장된 회사 목록을 조회
        List<CompanyEntity> companies = this.companyRespository.findAll();

        //회사마다 배당금 정보를 새로 스크래핑
        for (var company : companies) {
            log.info("scraping scheduler is started -> " + company.getName());
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(new Company(company.getTicker(), company.getName()));

            //스크랩핑한 배당금 정보 중 데이터베이스에 없는 값은 저장
            scrapedResult.getDividends().stream()
                    //디비든 모델을 디비든 엔티티로 매핑
                    .map(e -> new DividendEntity(company.getId(), e))
                    //엘리먼트를 하나씩 디비든 레파지토리에 삽입
                    .forEach(e -> {
                        boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if (!exists) {
                            this.dividendRepository.save(e);
                            log.info("insert new dividend -> " + e.toString());
                        }
                    });

            //this.dividendRepository.saveAll(e); saveAll은 유니크 컬럼이 있을 경우 에러 발생하므로 for문을 돌려 하나씩 검증

            //for문 db 조회 -> db 과부하 -> 검증 하나씩 할 때마다 일시정지
            //연속적으로 스크래핑 대상 사이트 서버에 요청을 하지 않도록 일시정지
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
