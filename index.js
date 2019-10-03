
import React, { PureComponent } from 'react';
import {requireNativeComponent, NativeModules, UIManager, findNodeHandle } from 'react-native';
import PropTypes from 'prop-types';

export default class FilterView extends PureComponent {

    constructor( props ) {
        super( props );
        this.saturation = UIManager.getViewManagerConfig( 'RNFilter' ).Commands.setSaturation
        this.contrast = UIManager.getViewManagerConfig( 'RNFilter' ).Commands.setContrast
        this.brightness = UIManager.getViewManagerConfig( 'RNFilter' ).Commands.setBrightness
        this.Filter = UIManager.getViewManagerConfig( 'RNFilter' ).Commands.setFilter
        this.genFilters = UIManager.getViewManagerConfig( 'RNFilter' ).Commands.generateFilters
        this.setSaturation = this._setSaturation.bind( this )
        this.setContrast = this._setContrast.bind( this )
        this.setBrightness = this._setBrightness.bind( this )
        this.setFilter = this._setFilter.bind( this )
        this.generateFilters = this._generateFilters.bind( this ),
        this.reqPromise  = null
    }

    componentDidMount() {
        this.props.onRef && this.props.onRef(this)
        this.viewHandle = findNodeHandle(this.filterRef);
    }


    _setSaturation( value ) {
        UIManager.dispatchViewManagerCommand(
            this.viewHandle,
            this.saturation,
            [ value ]
        );
    }
    
    _setBrightness( value ) {
        UIManager.dispatchViewManagerCommand(
            this.viewHandle,
            this.brightness,
            [ value ]
        );
    }
    
    _setContrast( value ) {
        UIManager.dispatchViewManagerCommand(
            this.viewHandle,
            this.contrast,
            [ value ]
        );
    }

    _setFilter( value ) {
        UIManager.dispatchViewManagerCommand(
            this.viewHandle,
            this.Filter,
            [ value ]
        );
    }

    _generateFilters( value ) {

        // const promise = new Promise( ( resolve, reject ) => {
        //     this.reqPromise = { resolve, reject };
            UIManager.dispatchViewManagerCommand(
                this.viewHandle,
                this.genFilters,
                [ ]
          );
    }

    onThumbsReturned = ({
        nativeEvent: { thumbs }
      }: {
        nativeEvent: { thumbs: thumbs }
      }) => {
        if(thumbs){
        this.props.onThumbsRecevied(thumbs)
        }
    }


    
    render() {
        return (
            <RNFilter
                style={ this.props.style }
                src={this.props.src}
                thumbnail={this.props.thumbnail}
                filter={this.props.filter}
                ref={ref => this.filterRef = ref}
                onDataReturned={this.onDataReturned}
                onThumbsReturned={this.onThumbsReturned}
            />
        )
    }
}

const RNFilter = requireNativeComponent("RNFilter", FilterView, {
    nativeOnly: { onDataReturned: true, onThumbNailsReturned: true  }
  });