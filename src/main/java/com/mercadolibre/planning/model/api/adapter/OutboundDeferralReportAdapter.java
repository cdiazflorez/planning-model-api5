package com.mercadolibre.planning.model.api.adapter;

import com.mercadolibre.planning.model.api.client.db.repository.deferral.OutboundDeferralDataRepository;
import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport.CptDeferralReport;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport.DeferralReportGateway;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OutboundDeferralReportAdapter implements DeferralReportGateway {

  private final OutboundDeferralDataRepository outboundDeferralDataRepository;

  /**
   * Deletes the deferral reports before the specified date.
   *
   * @param dateTo The date before which the deferral reports will be deleted.
   * @return The number of deferral reports deleted.
   */
  @Override
  public int deleteDeferralReportBeforeDate(Instant dateTo) {
    return outboundDeferralDataRepository.deleteByDateBefore(dateTo);
  }

  /**
   * Saves a deferral report with the provided CptDeferralReport list and date for logistic center.
   *
   * @param logisticCenterId The ID of the logistic center where the deferral report belongs.
   * @param deferralDate     The date of the deferral report.
   * @param cptDeferrals     The list of CptDeferralReport objects representing deferrals to be saved.
   */
  @Override
  public void saveDeferralReport(
      final String logisticCenterId,
      final Instant deferralDate,
      final List<CptDeferralReport> cptDeferrals
  ) {

    final List<OutboundDeferralData> outboundDeferralDataList = cptDeferrals.stream()
        .map(cpt -> new OutboundDeferralData(
            logisticCenterId,
            deferralDate,
            cpt.date(),
            cpt.status(),
            cpt.updated()
        )).collect(Collectors.toList());

    outboundDeferralDataRepository.saveAll(outboundDeferralDataList);
  }
}
