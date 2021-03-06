/* tslint:disable */
import { TppRedirectUri } from './tpp-redirect-uri';
export interface TppInfo {

  /**
   * Issuer CN
   */
  issuerCN: string;

  /**
   * Authorization number
   */
  authorisationNumber: string;

  /**
   * National competent authority name
   */
  authorityName: string;

  /**
   * Cancel TPP redirect URIs
   */
  cancelTppRedirectUri?: TppRedirectUri;

  /**
   * City
   */
  city: string;

  /**
   * Country
   */
  country: string;

  /**
   * National competent authority id
   */
  authorityId: string;

  /**
   * Organisation
   */
  organisation: string;

  /**
   * Organisation unit
   */
  organisationUnit: string;

  /**
   * State
   */
  state: string;

  /**
   * Tpp name
   */
  tppName: string;

  /**
   * Tpp role
   */
  tppRoles: Array<'PISP' | 'AISP' | 'PIISP' | 'ASPSP'>;
}
