import { Badge, ListItem, ListItemText } from '@material-ui/core';
import Collapse from '@material-ui/core/Collapse';
import List, { ListProps } from '@material-ui/core/List';
import { createStyles, Theme, withStyles, WithStyles } from '@material-ui/core/styles';
import React, { Component } from 'react';
import * as ConfigEditor from '../configEditor';

export interface MenuHeading {
  type: 'heading';
  text: string | React.ReactNode;
  offset?: number;
}

export interface MenuItem {
  type: 'item';
  name: string | React.ReactNode;
  slug?: string;
  ext?: string;
  onClick?: () => void;
  offset?: number;
  hasNotification?: boolean;
}

export interface MenuProject {
  type: 'project';
  projectId: string;
  page: ConfigEditor.Page;
  hasUnsavedChanges?: boolean;
}

const styles = (theme: Theme) => createStyles({
  badgeDot: {
    backgroundColor: theme.palette.text.primary,
  },
  childrenContainer: {
    position: 'relative',
  },
  childrenPadder: {
    marginLeft: 16,
    height: '100%',
    padding: '1px',
    position: 'absolute',
    borderRight: '1px solid rgba(224, 224, 224, 0.3)',
  },
});

interface Props extends ListProps {
  items: (MenuProject | MenuItem | MenuHeading)[];
  activePath: string;
  activeSubPath: ConfigEditor.Path;
  pageClicked: (path: string, subPath?: ConfigEditor.Path) => void;
}

export default class Menu extends Component<Props> {
  render() {
    return (
      <List dense component='nav' style={{ padding: '0px' }}>
        {this.props.items.map((item, index) => {
          if (item.type === 'item') {
            return (
              <ListItem key={`${index}-${item.slug || 'empty'}`} selected={item.slug === this.props.activePath} button onClick={() => {
                if (item.onClick) {
                  item.onClick();
                }
                if (item.slug !== undefined) {
                  this.props.pageClicked(item.slug);
                }
                if (item.ext !== undefined) {
                  window.open(item.ext, '_blank');
                }
              }}>
                <ListItemText style={Menu.paddingForLevel(item.offset)} primary={(
                  <React.Fragment>
                    <span>{item.name}</span>
                    <Badge
                      color='primary'
                      variant='dot'
                      invisible={!item.hasNotification}
                    >
                      &nbsp;&nbsp;
                    </Badge>
                  </React.Fragment>
                )} />
              </ListItem>
            );
          } else if (item.type === 'project') {
            return (
              <MenuPage
                key={`${index}-${item.page.key}`}
                page={item.page}
                hasUnsavedChanges={item.hasUnsavedChanges}
                activePath={item.projectId === this.props.activePath ? this.props.activeSubPath : undefined}
                pageClicked={path => this.props.pageClicked(item.projectId, path)}
              />
            );
          } else if (item.type === 'heading') {
            return (
              <ListItem key={`${index}-${item.text}`} disabled>
                <ListItemText style={Menu.paddingForLevel(item.offset)} primary={item.text} />
              </ListItem>
            );
          } else {
            return null;
          }
        })}
      </List>
    );
  }

  static paddingForLevel(offset: number = 0, path: ConfigEditor.Path = []): React.CSSProperties | undefined {
    const paddingLevel = path.length + offset;
    return paddingLevel === 0 ? undefined : { paddingLeft: paddingLevel * 10 };
  }
}

interface PropsPage {
  key: string;
  page: ConfigEditor.Page;
  activePath?: ConfigEditor.Path;
  pageClicked: (path: ConfigEditor.Path) => void;
  hasUnsavedChanges?: boolean;
}

class MenuPageWithoutStyle extends Component<PropsPage & WithStyles<typeof styles, true>> {
  unsubscribe?: () => void;

  componentDidMount() {
    this.unsubscribe = this.props.page.subscribe(this.forceUpdate.bind(this));
  }

  componentWillUnmount() {
    this.unsubscribe && this.unsubscribe();
  }

  render() {
    const expanded = this.isExpanded(this.props.page.path);
    const padding = Menu.paddingForLevel(1, this.props.page.path);
    const color = this.props.page.getColor();
    const { classes, ...menuProps } = this.props;
    return (
      <Collapse in={this.props.page.required || this.props.page.value === true} timeout="auto" unmountOnExit>
        <ListItem selected={this.isSelected(this.props.page.path)} button onClick={() => {
          this.props.pageClicked(this.props.page.path);
        }}>
          <ListItemText style={padding} primary={(
            <React.Fragment>
              <span style={{ color }}>
                {this.props.page.getDynamicName()}
              </span>
              <Badge
                variant='dot'
                invisible={!this.props.hasUnsavedChanges}
                color='primary'
              >
                &nbsp;&nbsp;
              </Badge>
            </React.Fragment>
          )} />
        </ListItem>
        <Collapse in={expanded} timeout="auto" unmountOnExit>
          {this.props.page.getChildren().all
            .map(child => {
              switch (child.type) {
                case ConfigEditor.PageType:
                  return (<MenuPage {...menuProps} hasUnsavedChanges={false} key={child.key} page={child} />);
                case ConfigEditor.PageGroupType:
                  return (<MenuPageGroup {...menuProps} key={child.key} pageGroup={child} />);
                default:
                  return null;
              }
            })}
        </Collapse>
      </Collapse>
    );
  }

  isExpanded(path: ConfigEditor.Path): boolean {
    if (!this.props.activePath || this.props.activePath.length < path.length) {
      return false;
    }
    for (let i = 0; i < path.length; i++) {
      if (path[i] !== this.props.activePath[i]) {
        return false;
      }
    }
    return true;
  }

  isSelected(path: ConfigEditor.Path) {
    if (!this.props.activePath || this.props.activePath.length !== path.length) {
      return false;
    }
    for (let i = 0; i < path.length; i++) {
      if (path[i] !== this.props.activePath[i]) {
        return false;
      }
    }
    return true;
  }
}
const MenuPage = withStyles(styles, { withTheme: true })(MenuPageWithoutStyle);

interface PropsPageGroup {
  key: string;
  pageGroup: ConfigEditor.PageGroup;
  activePath?: ConfigEditor.Path;
  pageClicked: (path: ConfigEditor.Path) => void;
}

class MenuPageGroupWithoutStyle extends Component<PropsPageGroup & WithStyles<typeof styles, true>> {
  unsubscribe?: () => void;

  componentDidMount() {
    this.unsubscribe = this.props.pageGroup.subscribe(this.forceUpdate.bind(this));
  }

  componentWillUnmount() {
    this.unsubscribe && this.unsubscribe();
  }

  render() {
    const childPages = this.props.pageGroup.getChildPages();
    const padding = Menu.paddingForLevel(1, this.props.pageGroup.path);
    const { classes, ...menuProps } = this.props;
    return (
      <Collapse in={childPages.length > 0} timeout="auto" unmountOnExit>
        <div>
          <ListItem disabled>
            <ListItemText
              style={padding}
              primary={this.props.pageGroup.name} />
          </ListItem>
          <div className={this.props.classes.childrenContainer}>
            <div className={this.props.classes.childrenPadder} style={padding} />
            {childPages.map(childPage =>
              <MenuPage {...menuProps} key={childPage.key} page={childPage} />
            )}
          </div>
        </div>
      </Collapse>
    );
  }
}
const MenuPageGroup = withStyles(styles, { withTheme: true })(MenuPageGroupWithoutStyle);
