/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StCheckboxModule, StPaginationModule, StTableModule, StSpinnerModule, StDropdownMenuModule, StSearchModule } from '@stratio/egeo';

import { SpTitleModule, MenuOptionsListModule, SpartaSidebarModule } from '@app/shared';
import { RouterModule } from "@angular/router";;

import { StoreModule } from '@ngrx/store';
import { reducers } from './reducers';
import { EffectsModule } from '@ngrx/effects';
import { ScheduledEffect } from './effects/scheduled';

import { SchedulerHelperService } from './services/scheduled-helper.service';
import { ScheduledSidebarModule } from './components/scheduled-sidebar/scheduled-sidebar.module';
import { ScheduledContainer } from './scheduled.container';
import { ScheduledComponent } from './scheduled.component';

@NgModule({
   imports: [
     CommonModule,
     MenuOptionsListModule,
     SpartaSidebarModule,
     StDropdownMenuModule,
     StCheckboxModule,
     StPaginationModule,
     StTableModule,
     StSpinnerModule,
     SpTitleModule,
     StTableModule,
     StSearchModule,
     ScheduledSidebarModule,
     StoreModule.forFeature('scheduled', reducers),
     EffectsModule.forFeature([ScheduledEffect]),
     RouterModule
   ],
   declarations: [ScheduledContainer, ScheduledComponent],
   exports: [ScheduledContainer],
   providers: [SchedulerHelperService]
})
export class ScheduledModule { }