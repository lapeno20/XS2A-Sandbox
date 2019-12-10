import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { catchError, map } from 'rxjs/operators';
import { CustomizeService } from './customize.service';

@Injectable({
  providedIn: 'root',
})
export class JsonService {
  customJsonSource = '../assets/UI/custom/jsons/';
  defaultJsonSource = '../assets/UI/jsons/';

  jsonLinks = {
    singlePayment: 'singlePayment.json',
    periodicPayment: 'periodicPayment.json',
    bulkPayment: 'bulkPayment.json',
    debtorAccount: 'debtorAccount.json',
    singlePaymentPlayWithData: 'playWithDataSinglePayment.json',
    dedicatedAccountsConsent: 'dedicatedAccountsConsent.json',
    bankOfferedConsent: 'bankOfferedConsent.json',
    globalConsent: 'globalConsent.json',
    availableAccountsConsent: 'availableAccountsConsent.json',
    availableAccountsConsentWithBalance:
      'availableAccountsConsentWithBalance.json',
    consent: 'consent.json',
    psuData: 'psuData.json',
    scaAuthenticationData: 'scaAuthenticationData.json',
    authenticationMethodId: 'authenticationMethodId.json',
  };

  private currency = 'EUR';

  constructor(
    private http: HttpClient,
    private customizeService: CustomizeService
  ) {
    this.customizeService.getJSON().then(data => {
      if (data.currency && data.currency.length !== 0) {
        this.currency = data.currency;
      }
    });
  }

  public getRawJsonData(url: string): Observable<any> {
    return this.http.get(this.customJsonSource + url).pipe(
      map(response => {
        return response;
      }),
      catchError(error => {
        return this.http.get(this.defaultJsonSource + url);
      })
    );
  }

  public getPreparedJsonData(url: string) {
    return this.getRawJsonData(url).pipe(
      map(data => {
        // replaces all the values of "currency" key word in formatted jsons
        const regex = /(?<="currency"\s*:\s*"\s*)(.+)(?=")/g;

        return JSON.parse(
          JSON.stringify(data, null, '\t').replace(regex, this.currency)
        );
      })
    );
  }
}
