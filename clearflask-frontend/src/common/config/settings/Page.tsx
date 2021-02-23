import loadable from '@loadable/component';
import { Typography } from '@material-ui/core';
import { createStyles, Theme, withStyles, WithStyles } from '@material-ui/core/styles';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { ReduxStateAdmin } from '../../../api/serverAdmin';
import Loading from '../../../app/utils/Loading';
import { importFailed, importSuccess } from '../../../Main';
import * as ConfigEditor from '../configEditor';
import CreditPreview from './injects/CreditPreview';
import PresetWidget from './PresetWidget';
import Property from './Property';
import { RestrictedProperties } from './UpgradeWrapper';

const WorkflowPreview = loadable(() => import(/* webpackChunkName: "WorkflowPreview" */'./injects/WorkflowPreview').then(importSuccess).catch(importFailed), { fallback: (<Loading />) });

const styles = (theme: Theme) => createStyles({
});
interface Props {
  key: string;
  page: ConfigEditor.Page;
  editor: ConfigEditor.Editor;
  pageClicked: (path: ConfigEditor.Path) => void;
}
interface ConnectProps {
  accountBasePlanId?: string;
}
class Page extends Component<Props & ConnectProps & WithStyles<typeof styles, true>> {
  unsubscribe?: () => void;

  componentDidMount() {
    this.unsubscribe = this.props.page.subscribe(this.forceUpdate.bind(this));
  }

  componentWillUnmount() {
    this.unsubscribe && this.unsubscribe();
  }

  render() {
    const creditPreview = this.props.page.pathStr === 'users.credits'
      && (<CreditPreview editor={this.props.editor} />);
    var workflowPreview;
    if (this.props.page.path.length > 0 && this.props.page.path[this.props.page.path.length - 1] === 'workflow') {
      workflowPreview = (
        <WorkflowPreview editor={this.props.editor} categoryIndex={this.props.page.path[2] as number} />
      );
    }

    var propertyRequiresUpgrade: ((propertyPath: ConfigEditor.Path) => boolean) | undefined;
    const restrictedProperties = this.props.accountBasePlanId && RestrictedProperties[this.props.accountBasePlanId];
    if (restrictedProperties) {
      propertyRequiresUpgrade = (path) => restrictedProperties.some(restrictedPath =>
        ConfigEditor.pathEquals(restrictedPath, path));
    }

    return (
      <div>
        <Typography variant='h4' component='h1'>{this.props.page.getDynamicName()}</Typography>
        <Typography variant='body1' component='p'>{this.props.page.description}</Typography>
        <PresetWidget page={this.props.page} editor={this.props.editor} />
        {creditPreview}
        {workflowPreview}
        {this.props.page.getChildren().all
          .filter(child => !(child as ConfigEditor.Property).hide)
          .map(child => (
            <Property
              key={child.key}
              prop={child}
              pageClicked={this.props.pageClicked}
              requiresUpgrade={propertyRequiresUpgrade}
              width='350px'
            />
          ))}
      </div>
    );
  }
}

export default connect<ConnectProps, {}, Props, ReduxStateAdmin>((state, ownProps) => {
  return {
    accountBasePlanId: state.account.account.account?.basePlanId,
  };
})(withStyles(styles, { withTheme: true })(Page));
