///
/// Copyright (C) 2015 Stratio (http://stratio.com)
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///         http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
    Component, OnInit, OnDestroy, ElementRef, Input, AfterContentInit,
    ChangeDetectorRef, Output, EventEmitter, ChangeDetectionStrategy, NgZone
} from '@angular/core';

import * as d3 from 'd3';
import { ENTITY_BOX } from '../../wizard.constants';
import { UtilsService } from '@app/shared/services/utils.service';
import { icons } from '@app/shared/constants/icons';
import {isMobile} from 'constants/global';


@Component({
    selector: '[wizard-node]',
    styleUrls: ['wizard-node.styles.scss'],
    templateUrl: 'wizard-node.template.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class WizardNodeComponent implements OnInit, OnDestroy, AfterContentInit {

    @Input() data: any;
    @Input() createdNew: boolean;
    @Input() selected: boolean;
    @Input() drawingConnectionStatus: any = {
        status: false,
        name: ''
    };

    @Output() onDrawConnector = new EventEmitter<any>();
    @Output() onFinishConnector = new EventEmitter<any>();

    private el: HTMLElement;
    private svg: any;
    private relationSelector: any;

    public boxConfig = ENTITY_BOX;
    public relationClasses = '';

    constructor(elementRef: ElementRef, private utilsService: UtilsService, private _cd: ChangeDetectorRef, private _ngZone: NgZone) {
        this.el = elementRef.nativeElement;
        this.svg = d3.select(this.el);
    }

    ngOnInit(): void {
        if (this.createdNew) {
            this.el.querySelector('.box').setAttribute('class', 'created-new');
        }
    }

    ngAfterContentInit() {
        setTimeout(() => {
            this.data.created = false;
        }, 400); //delete created opacity effect
        const data = this.data;

        this._ngZone.runOutsideAngular(() => {
            const textContainer = d3.select(this.el.querySelector('.text-container'));
            textContainer.append('text')
                .attr('x', 20)
                .attr('y', 35)
                .attr('class', 'entity-icon')
                .style('font-size', '25')
                .attr('fill', this.createdNew ? '#999' : '#0f1b27')
                .text(function (d) { return icons[data.classPrettyName] });

            if (data.hasErrors && !this.createdNew) {
                textContainer.append('text')
                    .attr('x', 116)
                    .attr('y', 22)
                    .attr('class', 'entity-icon2')
                    .style('font-size', '16')
                    .attr('fill', '#ec445c')
                    .text(function (d) { return '\uE613'; });
            }
        });

        this.relationSelector = d3.selectAll(this.el.querySelectorAll(('.relation')));
        switch (this.data.stepType) {
            case 'Input':
                this.relationClasses = 'output-point';
                this.generateEntry();
                break;
            case 'Output':
                this.relationClasses = 'entry-point';
                this.generateOutput();
                break;
            case 'Transformation':
                this.relationClasses = 'entry-point output-point';
                this.generateEntry(); this.generateOutput();
                break;
        }
    }

    generateEntry() {
        const that = this;
        this.relationSelector.on('mousedown', function () {
            if(that.drawingConnectionStatus.status) {
                return;
            }
            d3.select(this)
                .classed('over-output2', true);
            that.onDrawConnector.emit({
                event: d3.event,
                name: that.data.name
            });
            d3.event.stopPropagation();
        });
    }


    generateOutput() {
        this._ngZone.runOutsideAngular(() => {
            this.relationSelector
                .on('mouseover', function () {
                    d3.select(this)
                        .classed('over-output', true);
                }).on('mouseout', function () {
                    d3.select(this).classed('over-output', false);
                }).on('mouseup', () => {
                    this.onFinishConnector.emit(this.data);
                });

                if(isMobile) {
                    this.relationSelector.on('click', () => {
                        if(this.drawingConnectionStatus.status && this.drawingConnectionStatus.name !== this.data.name) {
                            this.onFinishConnector.emit(this.data);
                        }
                    });
                }

        });
    }

    ngOnDestroy(): void { }
}


