/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

import {CommonModule} from '@angular/common';
import { NgModule } from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';

import { WizardDetailsComponent } from './wizard-details.component';
import {NodeErrorsModule} from '@app/wizard/components/node-errors/node-errors.module';

@NgModule({
  exports: [
    WizardDetailsComponent
  ],
  declarations: [
    WizardDetailsComponent
  ],
  imports: [
    CommonModule,
    NodeErrorsModule,
    TranslateModule
  ]
})

export class WizardDetailsModule {}
