/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import { Component } from '@angular/core';
import { StHorizontalTab } from '@stratio/egeo';
import { Router } from '@angular/router';

@Component({
    selector: 'templates',
    templateUrl: 'templates.template.html',
    styleUrls: ['templates.styles.scss']
})

export class TemplatesComponent {

    public selectedOption = 'inputs';

    public tabsOptions: StHorizontalTab[] = [{
        id: 'inputs',
        text: 'Inputs',
    }, {
        id: 'transformations',
        text: 'Transformations',
    }, {
        id: 'outputs',
        text: 'Outputs'
    }];

    constructor(private _router: Router) { }

    ngOnInit(): void { }

    changeTabOption(option: StHorizontalTab) {
        this.selectedOption = option.id;
        this._router.navigate(['templates', option.id]);
    }

}
