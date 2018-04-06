/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import { NgModule } from '@angular/core';
import { StoreModule } from '@ngrx/store';
import { FormsModule } from '@angular/forms';
import { EffectsModule } from '@ngrx/effects';
import { StBreadcrumbsModule, StTableModule,
    StSearchModule, StCheckboxModule, StHorizontalTabsModule,
    StPaginationModule, StTooltipModule, StFullscreenLayoutModule
} from '@stratio/egeo';

import { SharedModule } from '@app/shared';
import {
    WorkflowsComponent,  WorkflowRouterModule, WorkflowDetailComponent,
    WorkflowExecutionInfoComponent, WorkflowsHeaderContainer, WorkflowsHeaderComponent,
    WorkflowsTableComponent, WorkflowsTableContainer
} from '.';

import { reducerToken, reducerProvider } from './reducers';
import { WorkflowEffect } from './effects/workflow';
import { SpTooltipModule } from '@app/shared/components/sp-tooltip/sp-tooltip.module';

@NgModule({
    declarations: [
        WorkflowsComponent,
        WorkflowDetailComponent,
        WorkflowExecutionInfoComponent,
        WorkflowsHeaderContainer,
        WorkflowsHeaderComponent,
        WorkflowsTableComponent,
        WorkflowsTableContainer
    ],
    imports: [
        FormsModule,
        SpTooltipModule,
        StoreModule.forFeature('workflows', reducerToken),
        EffectsModule.forFeature([WorkflowEffect]),
        StCheckboxModule,
        StHorizontalTabsModule,
        StFullscreenLayoutModule,
        StTableModule,
        StTooltipModule,
        StBreadcrumbsModule,
        StSearchModule,
        WorkflowRouterModule,
        StPaginationModule,
        SharedModule
    ],
    providers: [reducerProvider]
})

export class WorkflowsMonitoringModule {
}
