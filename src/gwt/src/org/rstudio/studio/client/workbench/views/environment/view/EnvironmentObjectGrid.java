package org.rstudio.studio.client.workbench.views.environment.view;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractCellTableBuilder;
import com.google.gwt.user.cellview.client.AbstractHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionModel;

public class EnvironmentObjectGrid extends EnvironmentObjectDisplay
{
   public interface Style extends CssResource
   {
      String objectGridColumn();
      String objectGridHeader();
      String checkColumn();
      String objectGrid();
      String valueColumn();
   }

   public interface Resources extends ClientBundle
   {
      @Source("EnvironmentObjectGrid.css")
      Style style();
   }

   public EnvironmentObjectGrid(EnvironmentObjectDisplay.Host host,
                                EnvironmentObjectsObserver observer)
   {
      super(host, observer);
      style_ = ((Resources)GWT.create(Resources.class)).style();
      style_.ensureInjected();
      selection_ = new MultiSelectionModel<RObjectEntry>(
              RObjectEntry.KEY_PROVIDER);
      
      createColumns();
      setTableBuilder(new EnvironmentObjectGridBuilder(this));
      setHeaderBuilder(new GridHeaderBuilder(this, false));
      setSkipRowHoverCheck(true);
      setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      setSelectionModel(selection_, 
         DefaultSelectionEventManager.<RObjectEntry>createCheckboxManager(0));
      addStyleName(style_.objectGrid());
   }

   // Returns the objects that should be considered selected. 
   // - If one or more objects are manually selected, that set of objects is 
   //   returned. 
   // - If no objects are manually selected but the list is filtered, the
   //   the objects that match the filter are returned.
   // - If no selection or filter is present, an empty list is returned
   //   (generally this causes operations to act on the whole list) 
   @Override
   public List<String> getSelectedObjects()
   {
      boolean hasFilter = !host_.getFilterText().isEmpty();
      ArrayList<String> selectedObjectNames = new ArrayList<String>();
      ArrayList<String> filteredObjectNames = new ArrayList<String>();
      List<RObjectEntry> objects = getVisibleItems();
      for (RObjectEntry object: objects)
      {
         if (object.visible)
         {
            if (hasFilter)
            {
               filteredObjectNames.add(object.rObject.getName());
            }
            if (selection_.isSelected(object))
            {
               selectedObjectNames.add(object.rObject.getName());
            }
         }
      }
      return selectedObjectNames.size() == 0 ? filteredObjectNames :
                                               selectedObjectNames;
   }

   private void createColumns()
   {
      checkColumn_ = new Column<RObjectEntry, Boolean>(
            new CheckboxCell(false, false))
            {
               @Override
               public Boolean getValue(RObjectEntry value)
               {
                  return selection_.isSelected(value); 
               }
            };
      addColumn(checkColumn_);
      checkHeader_ = new Header<Boolean>(new CheckboxCell())
      {
         @Override
         public Boolean getValue()
         {
            return selectAll_;
         }
      };
      checkHeader_.setUpdater(new ValueUpdater<Boolean>()
      {
         @Override
         public void update(Boolean value)
         {
            if (selectAll_ != value)
            {
               setSelectAll(value);
               selectAll_ = value;
            }
         }
      });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(filterRenderer_), "Name", 20, 
              ObjectGridColumn.COLUMN_NAME, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getName();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Type", 15, 
              ObjectGridColumn.COLUMN_TYPE, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getType();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Length", 10, 
              ObjectGridColumn.COLUMN_LENGTH, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return (new Integer(object.rObject.getLength())).toString();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Size", 15, 
              ObjectGridColumn.COLUMN_SIZE, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return StringUtil.formatFileSize(object.rObject.getSize());
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(filterRenderer_), "Value", 35, 
              ObjectGridColumn.COLUMN_VALUE, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.getDisplayValue();
                  }
              });
      for (Column<RObjectEntry, String> column: columns_)
      {
         addColumn(column);
      }
   }
   
   private void setSelectAll(boolean selected)
   {
      List<RObjectEntry> objects = getVisibleItems();
      for (RObjectEntry object: objects)
      {
         if (object.visible)
         {
            selection_.setSelected(object, selected);
         }
      }
   }

   private class GridHeaderBuilder 
           extends AbstractHeaderOrFooterBuilder<RObjectEntry>
   {
      public GridHeaderBuilder(AbstractCellTable<RObjectEntry> table,
                               boolean isFooter)
      {
         super(table, isFooter);
         setSortIconStartOfLine(false);
      }

      @Override
      protected boolean buildHeaderOrFooterImpl()
      {
         TableRowBuilder row = startRow();
         // Render an empty header cell for the check column
         TableCellBuilder selectAll = row.startTH();
         selectAll.className(style_.objectGridHeader() + " " +
                             style_.checkColumn());
         renderHeader(selectAll, new Cell.Context(0, 0, null), checkHeader_);
         selectAll.end();

         for (int i = 0; i < columns_.size(); i++)
         {
            ObjectGridColumn col = columns_.get(i);
            TableCellBuilder cell = row.startTH();
            cell.className(style_.objectGridHeader());
            cell.style().width(col.getWidth(), Unit.PCT);
            Cell.Context context = new Cell.Context(0, i, null);
            renderSortableHeader(cell, context, col.getHeader(), 
                  i == host_.getSortColumn(), 
                  host_.isAscendingSort());
            cell.endTH();
         }
         row.end();
         return true;
      }
   }

   // builds individual rows of the object table
   private class EnvironmentObjectGridBuilder
           extends AbstractCellTableBuilder<RObjectEntry>
   {

      public EnvironmentObjectGridBuilder(
            AbstractCellTable<RObjectEntry> cellTable)
      {
         super(cellTable);
      }

      @Override
      protected void buildRowImpl(RObjectEntry rowValue, int absRowIndex)
      {
         if (!rowValue.visible)
            return;

         TableRowBuilder row = startRow();

         TableCellBuilder check = row.startTD();
         check.className(style_.checkColumn());
         renderCell(check, createContext(0), checkColumn_, rowValue);
         check.endTD();

         for (int i = 0; i < columns_.size(); i++)
         {
            ObjectGridColumn col = columns_.get(i);
            TableCellBuilder td = row.startTD();
            String className = style_.objectGridColumn();
            if (col.getType() == ObjectGridColumn.COLUMN_VALUE)
            {
               className += " " + style_.valueColumn();
               td.title(rowValue.getDisplayValue());
            }
            if (col.getType() == ObjectGridColumn.COLUMN_NAME)
            {
               td.title(rowValue.rObject.getName());
            }
            td.className(className);
            td.style().width(col.getWidth(), Unit.PCT);
            renderCell(td, createContext(i+1), col, rowValue);
            td.endTD();
         }
         
         row.end();
      }
   }
   
   private Column<RObjectEntry, Boolean> checkColumn_;
   private Header<Boolean> checkHeader_;
   private ArrayList<ObjectGridColumn> columns_ = 
         new ArrayList<ObjectGridColumn>();
   private Style style_;
   private SelectionModel<RObjectEntry> selection_;
   private boolean selectAll_ = false;
}
